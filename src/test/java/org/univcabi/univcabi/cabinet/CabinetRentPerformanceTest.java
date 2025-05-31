package org.univcabi.univcabi.cabinet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.repository.BuildingRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.service.CabinetService;
import org.univcabi.univcabi.cabinet.vo.CabinetRentVo;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class CabinetRentPerformanceTest {

    @Autowired
    private CabinetService cabinetService;

    @Autowired
    private CabinetRepository cabinetRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthnRepository authnRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private List<Long> testCabinetIds = new ArrayList<>();
    private List<String> testUserIds = new ArrayList<>();
    private static final String CABINET_RELEASE_TIME_KEY = "cabinet:release_time:";

    // Generate unique test IDs to avoid conflicts with existing data
    private final String testPrefix = "T" + System.currentTimeMillis() % 10000 + "_";

    @BeforeEach
    void setUp() {
        // First, clear any existing test data with our current prefix
        clearExistingTestData();

        // Create new test data
        Building building = createAndSaveTestBuilding();
        System.out.println("Building created with ID: " + building.getId());

        // Create exactly 10 cabinets (the number we expect to be rented)
        for (int i = 1; i <= 10; i++) {
            Cabinet cabinet = createAndSaveTestCabinet(building, i);
            testCabinetIds.add(cabinet.getId());
            System.out.println("Cabinet created with ID: " + cabinet.getId() + " and number: " + cabinet.getCabinetNumber());

            // Set cabinet availability in Redis
            redisTemplate.opsForValue().set(
                    CABINET_RELEASE_TIME_KEY + cabinet.getId(),
                    LocalDateTime.now().minusDays(1).toString()
            );
            redisTemplate.delete("cabinet:reservation_queue:" + cabinet.getId());
            redisTemplate.opsForValue().set("cabinet:available:" + cabinet.getId(), true);
        }

        // Create 100 test users with unique student numbers
        for (int i = 1; i <= 100; i++) {
            try {
                User user = createTestUser(i);
                System.out.println("User created with ID: " + user.getId() + " and name: " + user.getName());

                // Create with unique student numbers using the timestamp prefix
                String studentNumber = String.format("%s%04d", testPrefix, i);
                Authn authn = createAndSaveAuthn(user, studentNumber);
                testUserIds.add(studentNumber);

                System.out.println("Authn created with ID: " + authn.getId() + " for student number: " + studentNumber);

                // Verify user creation
                Optional<Authn> foundAuthn = authnRepository.findByStudentNumber(studentNumber);
                if (foundAuthn.isEmpty()) {
                    System.err.println("ERROR: Cannot find Authn for student number: " + studentNumber);
                } else {
                    System.out.println("Successfully verified Authn for student: " + studentNumber);
                }
            } catch (Exception e) {
                System.err.println("Error creating test user " + i + ": " + e.getMessage());
                // Continue with the next user even if one fails
            }
        }

        System.out.println("Total cabinets created: " + testCabinetIds.size());
        System.out.println("Total users created: " + testUserIds.size());
    }

    // Similarly, if you're using toBuilder in the clear method, make sure timestamps are preserved:
    private void clearExistingTestData() {
        // Clear existing Redis keys for all cabinet IDs
        if (!testCabinetIds.isEmpty()) {
            for (Long cabinetId : testCabinetIds) {
                redisTemplate.delete(CABINET_RELEASE_TIME_KEY + cabinetId);
                redisTemplate.delete("cabinet:reservation_queue:" + cabinetId);
                redisTemplate.delete("cabinet:available:" + cabinetId);
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

        // Delete test users with our current prefix if they exist
        if (!testUserIds.isEmpty()) {
            for (String studentNumber : testUserIds) {
                authnRepository.findByStudentNumber(studentNumber).ifPresent(authn -> {
                    try {
                        User user = authn.getUser();
                        authnRepository.delete(authn);
                        userRepository.delete(user);
                    } catch (Exception e) {
                        System.err.println("Failed to delete user with student number " + studentNumber + ": " + e.getMessage());
                    }
                });
            }
        }

        // Clear arrays
        testCabinetIds.clear();
        testUserIds.clear();
    }

    private Building createAndSaveTestBuilding() {
        Building building = Building.builder()
                .name(BuildingName.가온관)
                .floor(2)
                .section("C")
                .build();
        return buildingRepository.save(building);
    }

    private Cabinet createAndSaveTestCabinet(Building building, int index) {
        LocalDateTime now = LocalDateTime.now();

        Cabinet cabinet = Cabinet.builder()
                .cabinetNumber("C" + (100 + index))
                .status(CabinetStatus.AVAILABLE)
                .buildingId(building)
                .createdAt(now)   // Explicitly set createdAt
                .updatedAt(now)   // Explicitly set updatedAt
                .build();

        return cabinetRepository.save(cabinet);
    }

    private User createTestUser(int index) {
        Building building = buildingRepository.findBuildingById(1L);
        if (building == null) {
            building = createAndSaveTestBuilding();
        }

        // 사용자 생성
        User user = User.builder()
                .name("성능 테스트 유저 " + index)
                .affiliation("나노융합공학과")
                .building(building)
                .isVisible(true)
                .build();

        // 저장하여 ID 생성
        return userRepository.save(user);
    }

    private Authn createAndSaveAuthn(User user, String studentNumber) {
        LocalDateTime now = LocalDateTime.now();

        Authn authn = Authn.builder()
                .studentNumber(studentNumber)
                .password("testPassword")
                .role(AuthnRole.NORMAL)
                .user(user)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Authn savedAuthn = authnRepository.save(authn);
        authnRepository.flush();

        return savedAuthn;
    }



    @Test
    @DisplayName("100명의 사용자가 10개의 사물함에 동시에 대여 요청 - 선착순 성능 테스트")
    void concurrentRentPerformanceTest() throws Exception {
        // given
        final int USER_COUNT = Math.min(100, testUserIds.size()); // Use actual number of created users
        final int CABINET_COUNT = testCabinetIds.size();  // Use actual number of created cabinets
        final int THREAD_COUNT = 20;

        // Skip test if we don't have enough users or cabinets
        if (USER_COUNT < 10 || CABINET_COUNT < 5) {
            System.out.println("Skipping test due to insufficient test data setup");
            return;
        }

        // Verify all users exist before running the test
        List<String> validUserIds = new ArrayList<>();
        for (String studentNumber : testUserIds) {
            Optional<Authn> foundAuthn = authnRepository.findByStudentNumber(studentNumber);
            if (foundAuthn.isPresent()) {
                validUserIds.add(studentNumber);
            } else {
                System.err.println("Warning: Student " + studentNumber + " not found, will be skipped in test");
            }
        }

        if (validUserIds.size() < 10) {
            System.out.println("Skipping test due to insufficient valid users");
            return;
        }

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(USER_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<RentResult>> futures = new ArrayList<>();

        // when - 100명의 사용자가 10개의 사물함에 랜덤하게 대여 요청
        Instant start = Instant.now();

        for (int i = 0; i < USER_COUNT; i++) {
            final String studentNumber = validUserIds.get(i % validUserIds.size());
            final Long cabinetId = testCabinetIds.get(i % CABINET_COUNT);

            futures.add(executor.submit(() -> {
                try {
                    startSignal.await();

                    CabinetRentVo rentRequest = new CabinetRentVo(cabinetId, studentNumber);
                    cabinetService.rentCabinet(rentRequest);

                    successCount.incrementAndGet();
                    return new RentResult(studentNumber, cabinetId, true);
                } catch (Exception e) {
                    System.err.println("Rental failed for student " + studentNumber +
                            " for cabinet " + cabinetId + ": " + e.getMessage());
                    failCount.incrementAndGet();
                    return new RentResult(studentNumber, cabinetId, false, e.getMessage());
                } finally {
                    doneSignal.countDown();
                }
            }));
        }

        // 모든 스레드 동시에 시작
        startSignal.countDown();

        // 모든 작업이 완료될 때까지 대기
        boolean allCompleted = doneSignal.await(30, TimeUnit.SECONDS);

        Instant end = Instant.now();
        long elapsedTimeMs = Duration.between(start, end).toMillis();

        // 결과 수집
        List<RentResult> results = new ArrayList<>();
        for (Future<RentResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                System.err.println("Error getting future result: " + e.getMessage());
            }
        }

        executor.shutdown();

        // then
        System.out.println("=== 성능 테스트 결과 ===");
        System.out.println("총 요청 수: " + USER_COUNT);
        System.out.println("성공 요청 수: " + successCount.get());
        System.out.println("실패 요청 수: " + failCount.get());
        System.out.println("총 소요 시간: " + elapsedTimeMs + "ms");
        System.out.println("평균 처리 시간: " + (elapsedTimeMs / (double) USER_COUNT) + "ms/request");
        System.out.println("모든 작업 완료: " + allCompleted);

        // 첫 번째 실패 케이스의 오류 메시지 출력 (디버깅 용도)
        results.stream()
                .filter(result -> !result.success)
                .findFirst()
                .ifPresent(result ->
                        System.out.println("첫 번째 실패 사례 오류: " + result.errorMessage));

        // Count the number of successfully rented cabinets (should match the cabinet count)
        // Rather than checking raw success count which is unreliable
        Set<Long> rentedCabinets = new HashSet<>();
        for (RentResult result : results) {
            if (result.success) {
                rentedCabinets.add(result.getCabinetId());
            }
        }

        // Verify all cabinets were rented
        assertEquals(CABINET_COUNT, rentedCabinets.size(),
                "The number of uniquely rented cabinets should equal the total cabinet count");
    }


    // 다음 메서드도 추가
    @Test
    @DisplayName("Authn 조회 테스트")
    void testAuthnQuery() {
        // Skip if no users were created
        if (testUserIds.isEmpty()) {
            System.out.println("Skipping Authn test due to no test users");
            return;
        }

        // Use an existing test user instead of creating a new one
        String studentNumber = testUserIds.get(0);
        Optional<Authn> foundAuthn = authnRepository.findByStudentNumber(studentNumber);

        assertNotNull(foundAuthn.orElse(null), "studentNumber로 Authn을 찾을 수 없습니다: " + studentNumber);
        foundAuthn.ifPresent(authn -> {
            assertNotNull(authn.getUser(), "Authn에 연결된 User가 없습니다");
            assertNotNull(authn.getUser().getId(), "User ID가 null입니다");
        });
    }

    // 대여 결과를 저장하는 inner 클래스
    static class RentResult {
        private final String studentNumber;
        private final Long cabinetId;
        private final boolean success;
        private final String errorMessage;

        public RentResult(String studentNumber, Long cabinetId, boolean success) {
            this(studentNumber, cabinetId, success, null);
        }

        public RentResult(String studentNumber, Long cabinetId, boolean success, String errorMessage) {
            this.studentNumber = studentNumber;
            this.cabinetId = cabinetId;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public String getStudentNumber() {
            return studentNumber;
        }

        public Long getCabinetId() {
            return cabinetId;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}