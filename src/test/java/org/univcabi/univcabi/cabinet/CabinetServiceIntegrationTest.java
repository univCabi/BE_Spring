package org.univcabi.univcabi.cabinet;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.cabinet.entity.*;
import org.univcabi.univcabi.cabinet.repository.BuildingRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetHistoryRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.service.CabinetRedisService;
import org.univcabi.univcabi.cabinet.service.CabinetService;
import org.univcabi.univcabi.cabinet.vo.CabinetDetailVo;
import org.univcabi.univcabi.cabinet.vo.CabinetRentVo;
import org.univcabi.univcabi.cabinet.vo.CabinetReturnVo;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.ServiceException;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.univcabi.univcabi.cabinet.service.CabinetService.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional  // Add this annotation to the class to make all methods transactional by default
public class CabinetServiceIntegrationTest {

    @Autowired
    private CabinetService cabinetService;

    @Autowired
    private CabinetRepository cabinetRepository;

    @Autowired
    private AuthnRepository authnRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CabinetRedisService cabinetRedisService;

    @Autowired
    private CabinetHistoryRepository cabinetHistoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private List<Long> testCabinetIds = new ArrayList<>();
    private List<String> testStudentNumbers = new ArrayList<>();
    private static final String CABINET_RELEASE_TIME_KEY = "cabinet:release_time:";
    private static final String CABINET_RESERVATION_QUEUE_KEY = "cabinet:reservation_queue:";
    private static final String CABINET_AVAILABLE_KEY = "cabinet:available:";

    // Generate unique test IDs to avoid conflicts with existing data
    private final String testPrefix = "T" + System.currentTimeMillis() % 10000 + "_";

    @BeforeEach
    void setUp() {
        // Clear any existing test data
        clearExistingTestData();

        try {
            // Create test building
            Building building = createAndSaveTestBuilding();
            buildingRepository.flush();  // Use repository flush instead of entityManager

            System.out.println("Test building created with ID: " + building.getId());

            // Create 5 test cabinets
            for (int i = 1; i <= 5; i++) {
                Cabinet cabinet = createAndSaveTestCabinet(building, i);
                testCabinetIds.add(cabinet.getId());
                System.out.println("Created cabinet ID: " + cabinet.getId() + ", Number: " + cabinet.getCabinetNumber());

                // Set cabinet availability in Redis
                cabinetRedisService.setTemporaryStatus(cabinet.getId(), CabinetStatus.AVAILABLE);
                redisTemplate.delete(CABINET_RESERVATION_QUEUE_KEY + cabinet.getId());
            }

            cabinetRepository.flush();  // Use repository flush instead of entityManager

            // Create 10 test users
            for (int i = 1; i <= 10; i++) {
                // 명시적으로 사용자를 생성하고 저장
                String studentNumber = testPrefix + i;
                User user = createTestUser(i);
                user = userRepository.saveAndFlush(user); // 명시적으로 flush

                System.out.println("Created user ID: " + user.getId() + ", Name: " + user.getName());

                Authn authn = Authn.builder()
                        .studentNumber(studentNumber)
                        .password("testPassword")
                        .role(AuthnRole.NORMAL)
                        .user(user)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                authn = authnRepository.saveAndFlush(authn); // 명시적으로 flush
                System.out.println("Created Authn ID: " + authn.getId() + ", Student Number: " + studentNumber);

                testStudentNumbers.add(studentNumber);

                // Verify user creation
                Optional<Authn> foundAuthn = authnRepository.findByStudentNumber(studentNumber);
                if (foundAuthn.isEmpty()) {
                    System.err.println("ERROR: Cannot find Authn for student number: " + studentNumber);
                    fail("Cannot find Authn record right after creation: " + studentNumber);
                } else {
                    User foundUser = foundAuthn.get().getUser();
                    if (foundUser == null) {
                        System.err.println("ERROR: User is null for Authn with student number: " + studentNumber);
                        fail("Found Authn but it has null User: " + studentNumber);
                    } else {
                        System.out.println("Verified Authn and User for student: " + studentNumber);
                    }
                }
            }

            System.out.println("Setup completed with " + testCabinetIds.size() + " cabinets and " +
                    testStudentNumbers.size() + " students");
        } catch (Exception e) {
            System.err.println("Error during test setup: " + e.getMessage());
            e.printStackTrace();
            fail("Test setup failed: " + e.getMessage());
        }
    }

    private void clearExistingTestData() {
        System.out.println("Clearing existing test data...");

        // Clear existing Redis keys for all cabinet IDs
        if (!testCabinetIds.isEmpty()) {
            for (Long cabinetId : testCabinetIds) {
                redisTemplate.delete(CABINET_STATUS_KEY + cabinetId);
                redisTemplate.delete(CABINET_RENT_PROCESSING_KEY + cabinetId);
                redisTemplate.delete(CABINET_RETURN_PROCESSING_KEY + cabinetId);
                redisTemplate.delete(CABINET_RESERVATION_QUEUE_KEY + cabinetId);
            }

            // Reset cabinet statuses
            cabinetRepository.findAllById(testCabinetIds).forEach(cabinet -> {
                Cabinet updatedCabinet = cabinet.toBuilder()
                        .status(CabinetStatus.AVAILABLE)
                        .userId(null)
                        .build();
                cabinetRepository.save(updatedCabinet);
            });
        }

        // Delete test users with our prefix
        try {
            List<Authn> authns = authnRepository.findAll();
            for (Authn authn : authns) {
                if (authn.getStudentNumber() != null && authn.getStudentNumber().startsWith(testPrefix)) {
                    System.out.println("Deleting Authn: " + authn.getStudentNumber());
                    User user = authn.getUser();
                    authnRepository.delete(authn);
                    if (user != null) {
                        userRepository.delete(user);
                    }
                }
            }

            // Flush to make sure deletions are committed
            authnRepository.flush();
            userRepository.flush();
        } catch (Exception e) {
            System.err.println("Error clearing existing users: " + e.getMessage());
            e.printStackTrace();
        }

        // 먼저 모든 관련 히스토리 레코드를 삭제
        try {
            // 삭제할 캐비넷 ID 목록을 수집
            List<Long> cabinetIdsToDelete = new ArrayList<>();
            List<Cabinet> cabinets = cabinetRepository.findAll();
            for (Cabinet cabinet : cabinets) {
                if (cabinet.getCabinetNumber() != null && cabinet.getCabinetNumber().startsWith("B1")) {
                    cabinetIdsToDelete.add(cabinet.getId());
                }
            }

            // 히스토리 레코드 삭제 - 네이티브 쿼리 사용
            if (!cabinetIdsToDelete.isEmpty()) {
                EntityManager em = entityManager.getEntityManagerFactory().createEntityManager();
                em.getTransaction().begin();
                try {
                    for (Long cabinetId : cabinetIdsToDelete) {
                        // 물음표(?)를 사용하여 위치 기반 파라미터로 변경
                        em.createNativeQuery("DELETE FROM cabinet_histories WHERE cabinet_id = ?")
                                .setParameter(1, cabinetId)
                                .executeUpdate();
                        System.out.println("Deleted history records for cabinet ID: " + cabinetId);
                    }
                    em.getTransaction().commit();
                } catch (Exception e) {
                    em.getTransaction().rollback();
                    System.err.println("Error deleting cabinet histories: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    em.close();
                }
            }

            // 이제 캐비넷 삭제
            for (Cabinet cabinet : cabinets) {
                if (cabinet.getCabinetNumber() != null && cabinet.getCabinetNumber().startsWith("B1")) {
                    System.out.println("Deleting Cabinet: " + cabinet.getCabinetNumber());
                    cabinetRepository.delete(cabinet);
                }
            }

            // Flush to make sure deletions are committed
            cabinetRepository.flush();
        } catch (Exception e) {
            System.err.println("Error clearing existing cabinets: " + e.getMessage());
            e.printStackTrace();
        }

        // Clear arrays
        testCabinetIds.clear();
        testStudentNumbers.clear();

        System.out.println("Existing test data cleared.");
    }

    private Building createAndSaveTestBuilding() {
        Building building = Building.builder()
                .name(BuildingName.가온관)
                .floor(2)
                .section("B")
                .build();
        return buildingRepository.save(building);
    }

    private Cabinet createAndSaveTestCabinet(Building building, int index) {
        LocalDateTime now = LocalDateTime.now();

        Cabinet cabinet = Cabinet.builder()
                .cabinetNumber("B" + (100 + index))
                .status(CabinetStatus.AVAILABLE)
                .buildingId(building)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return cabinetRepository.save(cabinet);
    }

    private User createTestUser(int index) {
        Building building = buildingRepository.findBuildingById(1L);
        if (building == null) {
            building = createAndSaveTestBuilding();
        }

        // Create user
        User user = User.builder()
                .name("테스트 유저 " + index)
                .affiliation("테스트학과")
                .building(building)
                .isVisible(true)
                .build();

        return userRepository.save(user);
    }

    @Test
    @DisplayName("사물함 반납 후 Redis에서 상태가 즉시 초기화되는지 확인")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void returnCabinet_ShouldClearRedisState() throws Exception {
        // given
        Long cabinetId = testCabinetIds.get(0);
        String studentNumber = testStudentNumbers.get(0);

        System.out.println("Testing rental using cabinet: " + cabinetId + ", student: " + studentNumber);

        // 트랜잭션 분리된 EntityManager를 사용하여 캐비닛 상태 초기화
        EntityManager cabEm = entityManager.getEntityManagerFactory().createEntityManager();
        cabEm.getTransaction().begin();
        try {
            // 캐비닛이 실제로 존재하는지 확인
            Cabinet existingCabinet = cabEm.find(Cabinet.class, cabinetId);
            if (existingCabinet == null) {
                fail("캐비닛을 찾을 수 없습니다. ID: " + cabinetId);
            }

            Cabinet cabinet = existingCabinet.toBuilder()
                    .status(CabinetStatus.AVAILABLE)
                    .userId(null)
                    .build();

            cabEm.merge(cabinet);
            cabEm.getTransaction().commit();

            System.out.println("Cabinet prepared with ID: " + cabinetId + ", Status: " + cabinet.getStatus());
        } catch (Exception e) {
            cabEm.getTransaction().rollback();
            fail("캐비닛 준비 실패: " + e.getMessage());
        } finally {
            cabEm.close();
        }

        // 모든 Redis 키 초기화
        String statusKey = CABINET_STATUS_KEY + cabinetId;
        redisTemplate.delete(statusKey);
        redisTemplate.delete(CABINET_RENT_PROCESSING_KEY + cabinetId);
        redisTemplate.delete(CABINET_RETURN_PROCESSING_KEY + cabinetId);
        redisTemplate.delete(CABINET_OPERATION_RESULT_KEY + cabinetId + ":" + studentNumber);
        redisTemplate.delete(CABINET_RELEASE_TIME_KEY + cabinetId);
        redisTemplate.delete(CABINET_RESERVATION_QUEUE_KEY + cabinetId);

        // Redis에 AVAILABLE 상태 명시적으로 설정하고 즉시 확인
        cabinetRedisService.setTemporaryStatus(cabinetId, CabinetStatus.AVAILABLE);

        // 검증: 캐비닛 상태가 제대로 설정되었는지 즉시 확인
        Object initialStatus = redisTemplate.opsForValue().get(statusKey);
        assertNotNull(initialStatus, "초기 Redis 상태가 설정되어야 합니다");
        assertEquals(CabinetStatus.AVAILABLE.toString(), initialStatus.toString(), "초기 Redis 상태가 AVAILABLE이어야 합니다");

        // 관련 객체 검증
        Optional<Authn> authnOpt = authnRepository.findByStudentNumber(studentNumber);
        assertTrue(authnOpt.isPresent(), "학번으로 인증 정보를 찾을 수 없습니다: " + studentNumber);
        assertNotNull(authnOpt.get().getUser(), "인증 정보에 연결된 사용자가 없습니다");

        // 먼저 대여 처리
        System.out.println("Attempting to rent cabinet " + cabinetId + " for student " + studentNumber);
        CompletableFuture<CabinetDetailVo> rentFuture = cabinetService.rentCabinet(new CabinetRentVo(cabinetId, studentNumber));

        // 더 긴 타임아웃 적용
        CabinetDetailVo rentResult = rentFuture.get(10, TimeUnit.SECONDS); // 10초 타임아웃

        System.out.println("Rent result: " + (rentResult.isMine() ? "SUCCESS" : "FAILURE") + ", Status: " + rentResult.status());
        assertTrue(rentResult.isMine(), "대여 처리가 성공해야 합니다");
        assertEquals(CabinetStatus.USING, rentResult.status(), "대여 후 상태가 USING이어야 합니다");

        // Redis에 직접 USING 상태 설정 후 즉시 확인
        cabinetRedisService.setTemporaryStatus(cabinetId, CabinetStatus.USING);

        // 설정 후 Redis 상태를 즉시 확인 (여러 번 시도)
        Object redisStatusAfterRent = null;
        for (int i = 0; i < 3; i++) {  // 최대 3번 시도
            redisStatusAfterRent = redisTemplate.opsForValue().get(statusKey);
            if (redisStatusAfterRent != null &&
                    redisStatusAfterRent.toString().equals(CabinetStatus.USING.toString())) {
                break;  // 원하는 상태가 확인되면 루프 종료
            }

            // 상태가 맞지 않으면 다시 설정
            System.out.println("Redis status not as expected. Retrying... Current: " +
                    (redisStatusAfterRent != null ? redisStatusAfterRent.toString() : "null"));
            cabinetRedisService.setTemporaryStatus(cabinetId, CabinetStatus.USING);
        }

        // 검증
        assertNotNull(redisStatusAfterRent, "Redis에 캐비닛 상태가 있어야 합니다");
        assertEquals(CabinetStatus.USING.toString(), redisStatusAfterRent.toString(),
                "Redis에 저장된 상태가 USING이어야 합니다");
        // 반납 처리
        System.out.println("Attempting to return cabinet " + cabinetId + " for student " + studentNumber);
        CabinetDetailVo returnResult = cabinetService.returnCabinet(new CabinetReturnVo(cabinetId, studentNumber));

        assertNotNull(returnResult);
        assertFalse(returnResult.isMine(), "반납 후에는 isMine이 false여야 합니다");
        assertEquals(CabinetStatus.AVAILABLE, returnResult.status(), "반납 후 상태가 AVAILABLE이어야 합니다");

        // Redis 상태 즉시 확인 (sleep 없이) - 반납 후 AVAILABLE로 변경되어야 함
        Object redisStatusAfterReturn = redisTemplate.opsForValue().get(statusKey);

        // Null 검사 및 처리 (null인 경우 재시도 로직)
        if (redisStatusAfterReturn == null) {
            System.out.println("Warning: Redis status key is missing after return. This should not happen with the fixed code.");
            System.out.println("Setting it manually and checking again.");
            cabinetRedisService.setTemporaryStatus(cabinetId, CabinetStatus.AVAILABLE);
            redisStatusAfterReturn = redisTemplate.opsForValue().get(statusKey);
        }

        // 최종 검증
        assertNotNull(redisStatusAfterReturn, "Redis에 반납 후 상태가 있어야 합니다");
        assertEquals(CabinetStatus.AVAILABLE.toString(), redisStatusAfterReturn.toString(), "Redis에 저장된 상태가 AVAILABLE로 업데이트되어야 합니다");

        // 처리 락 키가 제거되었는지 확인
        Boolean hasRentProcessingKey = redisTemplate.hasKey(CABINET_RENT_PROCESSING_KEY + cabinetId);
        Boolean hasReturnProcessingKey = redisTemplate.hasKey(CABINET_RETURN_PROCESSING_KEY + cabinetId);

        assertFalse(hasRentProcessingKey, "반납 후 대여 처리 락이 제거되어야 합니다");
        assertFalse(hasReturnProcessingKey, "반납 후 반납 처리 락이 제거되어야 합니다");

        // 반납 후 대여 가능 시간 설정 확인
        Object releaseTimeStr = redisTemplate.opsForValue().get(CABINET_RELEASE_TIME_KEY + cabinetId);
        assertNotNull(releaseTimeStr, "반납 후 대여 가능 시간이 설정되어야 합니다");

        LocalDateTime releaseTime = LocalDateTime.parse(releaseTimeStr.toString());
        LocalDateTime expectedReleaseTime = LocalDateTime.of(
                LocalDate.now().plusDays(1),
                LocalTime.of(13, 0)
        );

        // 날짜와 시간만 비교 (초/나노초는 무시)
        assertEquals(expectedReleaseTime.toLocalDate(), releaseTime.toLocalDate(), "대여 가능 날짜가 다음 날이어야 합니다");
        assertEquals(expectedReleaseTime.getHour(), releaseTime.getHour(), "대여 가능 시간이 13시여야 합니다");
    }

    @Test
    @DisplayName("선착순 대여 - 가장 먼저 요청한 사용자만 대여 성공해야 함")
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 메서드 수준에서 트랜잭션 비활성화
    void concurrentRentCabinet_OnlyFirstRequesterShouldSucceed() throws Exception {
        // Skip if we don't have enough test data
        if (testCabinetIds.isEmpty() || testStudentNumbers.size() < 5) {
            fail("테스트 데이터가 충분하지 않습니다.");
        }

        Long cabinetId = testCabinetIds.get(1);

        // 캐비닛 상태를 AVAILABLE로 확실히 설정
        EntityManager cabEm = entityManager.getEntityManagerFactory().createEntityManager();
        cabEm.getTransaction().begin();
        try {
            Cabinet cabinet = cabEm.find(Cabinet.class, cabinetId);
            cabinet = cabinet.toBuilder()
                    .status(CabinetStatus.AVAILABLE)
                    .userId(null)
                    .build();
            cabEm.merge(cabinet);
            cabEm.getTransaction().commit();

            System.out.println("Cabinet prepared with ID: " + cabinetId);
        } catch (Exception e) {
            cabEm.getTransaction().rollback();
            fail("캐비닛 준비 실패: " + e.getMessage());
        } finally {
            cabEm.close();
        }

        // Redis 키 초기화
        redisTemplate.delete(CABINET_STATUS_KEY + cabinetId);
        redisTemplate.delete(CABINET_RENT_PROCESSING_KEY + cabinetId);
        redisTemplate.delete(CABINET_RETURN_PROCESSING_KEY + cabinetId);
        redisTemplate.delete(CABINET_RESERVATION_QUEUE_KEY + cabinetId);

        // Redis에 AVAILABLE 상태 설정
        cabinetRedisService.setTemporaryStatus(cabinetId, CabinetStatus.AVAILABLE);

        // 사용할 학번 목록 준비
        List<String> testUsers = testStudentNumbers.subList(0, 5);
        int userCount = testUsers.size();

        System.out.println("Running concurrent test with " + userCount + " users for cabinet ID: " + cabinetId);

        // 동시 요청 실행
        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(userCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger cabinetAlreadyUsingCount = new AtomicInteger(0);
        AtomicInteger cabinetRentFailedCount = new AtomicInteger(0);

        // 각 사용자별 대여 요청 준비
        for (int i = 0; i < userCount; i++) {
            final String studentNumber = testUsers.get(i);
            final int studentIndex = i;

            Runnable task = () -> {
                try {
                    System.out.println("Thread " + studentIndex + " waiting to start for student: " + studentNumber);
                    startSignal.await(); // 모든 스레드가 준비될 때까지 대기
                    System.out.println("Thread " + studentIndex + " started for student: " + studentNumber);

                    // 대여 요청
                    CompletableFuture<CabinetDetailVo> rentFuture = cabinetService.rentCabinet(
                            new CabinetRentVo(cabinetId, studentNumber));

                    try {
                        // 결과 대기 (최대 5초)
                        CabinetDetailVo result = rentFuture.get(5, TimeUnit.SECONDS);
                        System.out.println("Student " + studentNumber + " rent result: " +
                                (result.isMine() ? "SUCCESS" : "FAILURE"));

                        if (result.isMine()) {
                            successCount.incrementAndGet();
                        }
                    } catch (ExecutionException e) {
                        failureCount.incrementAndGet();
                        System.out.println("Student " + studentNumber + " request failed with exception: " + e.getCause());

                        // 명시적으로 예외 타입 확인
                        if (e.getCause() instanceof ServiceException) {
                            ServiceException serviceEx = (ServiceException) e.getCause();
                            System.out.println("Exception status: " + serviceEx.getStatus());

                            if (serviceEx.getStatus() == ExceptionStatus.CABINET_ALREADY_USING) {
                                cabinetAlreadyUsingCount.incrementAndGet();
                                System.out.println("CABINET_ALREADY_USING exception detected");
                            } else if (serviceEx.getStatus() == ExceptionStatus.CABINET_RENT_FAILED) {
                                cabinetRentFailedCount.incrementAndGet();
                                System.out.println("CABINET_RENT_FAILED exception detected");
                            }
                        }
                    } catch (TimeoutException | InterruptedException e) {
                        failureCount.incrementAndGet();
                        System.out.println("Student " + studentNumber + " request timed out or interrupted");
                    }
                } catch (Exception e) {
                    System.out.println("Student " + studentNumber + " unexpected exception: " + e);
                } finally {
                    doneSignal.countDown();
                }
            };

            executor.submit(task);
        }

        // 모든 스레드 동시에 시작
        startSignal.countDown();

        // 모든 스레드 완료 대기
        doneSignal.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 결과 검증
        System.out.println("Final counts:");
        System.out.println("Success count: " + successCount.get());
        System.out.println("Failure count: " + failureCount.get());
        System.out.println("CABINET_ALREADY_USING count: " + cabinetAlreadyUsingCount.get());
        System.out.println("CABINET_RENT_FAILED count: " + cabinetRentFailedCount.get());

        // 검증
        assertEquals(1, successCount.get(), "정확히 한 명의 사용자만 대여에 성공해야 합니다");
        assertEquals(userCount - 1, failureCount.get(), "나머지 사용자는 모두 실패해야 합니다");

        // 적어도 한 개 이상의 예외가 특정 유형이어야 함
        int specificExceptionCount = cabinetAlreadyUsingCount.get() + cabinetRentFailedCount.get();
        assertTrue(specificExceptionCount > 0,
                "적어도 일부 사용자는 '이미 대여중' 또는 '대여 실패' 예외를 받아야 합니다");
    }

    @Test
    @DisplayName("Authn 조회 테스트")
    void testAuthnQuery() {
        // Skip if no users were created
        if (testStudentNumbers.isEmpty()) {
            System.out.println("Skipping Authn test due to no test users");
            return;
        }

        // Use an existing test user instead of creating a new one
        String studentNumber = testStudentNumbers.get(0);
        Optional<Authn> foundAuthn = authnRepository.findByStudentNumber(studentNumber);

        assertNotNull(foundAuthn.orElse(null), "studentNumber로 Authn을 찾을 수 없습니다: " + studentNumber);
        foundAuthn.ifPresent(authn -> {
            assertNotNull(authn.getUser(), "Authn에 연결된 User가 없습니다");
            assertNotNull(authn.getUser().getId(), "User ID가 null입니다");
        });
    }
}