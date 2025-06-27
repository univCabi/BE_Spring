package org.univcabi.univcabi.cabinet.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.cabinet.dto.CabinetKafkaDto;
import org.univcabi.univcabi.cabinet.entity.*;
import org.univcabi.univcabi.cabinet.repository.BuildingRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetHistoryRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetPositionRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.vo.*;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.ServiceException;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;



@Service
public class CabinetService {
    private static final Logger logger = LoggerFactory.getLogger(CabinetService.class);

    private final CabinetRepository cabinetRepository;
    private final CabinetPositionRepository cabinetPositionRepository;
    private final CabinetHistoryRepository cabinetHistoryRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final AuthnRepository authnRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CabinetKafkaProducerService kafkaProducerService;
    private final ReservationQueueManager queueManager;
    private final CabinetUtilService cabinetUtilService;
    private final CabinetRedisService cabinetRedisService;

    private final Executor cabinetTaskExecutor;


    // Redis 키 상수

    public static final String CABINET_RENT_PROCESSING_KEY = "cabinet:processing:rent:";
    public static final String CABINET_RETURN_PROCESSING_KEY = "cabinet:processing:return:";
    public static final String CABINET_STATUS_KEY = "cabinet:status:";
    public static final String CABINET_OPERATION_RESULT_KEY = "cabinet:operation:result:";
    public static final String CABINET_OPERATION_CHANNEL = "cabinet:operation:channel";
    private final CabinetFallbackService cabinetFallbackService;

    public CabinetService(
            CabinetRepository cabinetRepository,
            UserRepository userRepository,
            CabinetPositionRepository cabinetPositionRepository,
            CabinetHistoryRepository cabinetHistoryRepository,
            BuildingRepository buildingRepository,
            AuthnRepository authnRepository,
            RedisTemplate<String, Object> redisTemplate,
            CabinetKafkaProducerService kafkaProducerService,
            ReservationQueueManager queueManager,
            CabinetUtilService cabinetUtilService,
            CabinetRedisService cabinetRedisService,
            Executor cabinetTaskExecutor, CabinetFallbackService cabinetFallbackService) {
        this.cabinetRepository = cabinetRepository;
        this.userRepository = userRepository;
        this.cabinetPositionRepository = cabinetPositionRepository;
        this.cabinetHistoryRepository = cabinetHistoryRepository;
        this.buildingRepository =buildingRepository;
        this.authnRepository = authnRepository;
        this.redisTemplate = redisTemplate;
        this.kafkaProducerService = kafkaProducerService;
        this.queueManager = queueManager;
        this.cabinetUtilService = cabinetUtilService;
        this.cabinetRedisService = cabinetRedisService;
        this.cabinetTaskExecutor = cabinetTaskExecutor;
        this.cabinetFallbackService = cabinetFallbackService;
    }

    @PostConstruct
    public void init() {
        // At startup, clear any stale processing locks
        cabinetRedisService.clearAllProcessingLocks();
    }

    public Page<CabinetVo> findAllCabinetInfo(CabinetPageVo requestVo) {
        Pageable pageable = PageRequest.of(
                requestVo.page(),
                requestVo.pageSize()
        );

        Page<Cabinet> cabinetPage = cabinetRepository.findAllCabinetInfo(pageable);

        // Page<Cabinet>을 Page<CabinetVo>로 변환
        return cabinetPage.map(cabinet -> new CabinetVo(
                cabinet.getBuildingId().getName(),
                cabinet.getBuildingId().getFloor(),
                cabinet.getCabinetNumber()
        ));
    }

    public List<CabinetDataVo> findCabinetsByBuildingAndFloor(CabinetLocationVo requestVo){
        List<Cabinet> cabinetList  =  cabinetRepository.findCabinetByBuildingAndFloor(
                requestVo.building(),
                requestVo.floors()
        );

        return cabinetList.stream()
                .map(cabinet -> {

                    CabinetPosition cabinetPosition = cabinetPositionRepository.findByCabinetId(cabinet)
                            .orElseThrow(()-> new ServiceException(ExceptionStatus.CABINET_POSITION_NOT_FOUND));

                    User user = cabinet.getUserId();
                    // 유무료는 언제든 조건이 바뀔 수 있으니 초기 선언
                    boolean isFree = true;

                    return new CabinetDataVo(
                            cabinet.getId(),
                            cabinet.getCabinetNumber(),
                            cabinetPosition.getCabinetXPos(),
                            cabinetPosition.getCabinetYPos(),
                            cabinet.getStatus(),
                            user!= null ? user.getIsVisible() : false,
                            user!= null? user.getName() : null,
                            user != null && Objects.equals(user.getAuthn().getStudentNumber(), requestVo.studentNumber()),
                            cabinet.getStatus() == CabinetStatus.AVAILABLE,
                            isFree
                    );
                }).toList();

    }

    public CabinetDetailVo findOneCabinetInfo(CabinetFindOneVo requestVo) {
        // 1. 캐비닛 조회
        Optional<Cabinet> cabinetOptional = cabinetRepository.findOneCabinetInfoByCabinetId(requestVo.cabinetId());

        if (cabinetOptional.isEmpty()) {
            throw new RuntimeException("캐비닛을 찾을 수 없습니다.");
        }

        Cabinet cabinet = cabinetOptional.get();
        Building building = cabinet.getBuildingId();
        User cabinetOwner = cabinet.getUserId();

        // 2. studentNumber로 요청자 확인 (isMine 여부 판단)
        boolean isMine = cabinetUtilService.checkIsMine(requestVo.studentNumber(), cabinetOwner);

        //TODO: 왜 Boolean 이거여야 하는지 알아보기
        Boolean isVisible = (cabinetOwner != null) ? cabinetOwner.getIsVisible() : false;

        // 3. Entity를 VO로 변환
        return new CabinetDetailVo(
                building.getFloor(),
                cabinet.getCabinetNumber().substring(0, 1), // section
                building.getName(),
                cabinet.getCabinetNumber(),
                cabinet.getStatus(),
                isVisible,
                cabinetOwner != null ? cabinetOwner.getName() : null,
                isMine,
                cabinet.getUpdatedAt() // 만료일
        );
    }

    public CompletableFuture<CabinetDetailVo> rentCabinet(CabinetRentVo requestVo) {
        Long cabinetId = requestVo.cabinetId();
        String studentNumber = requestVo.studentNumber();

        // 1. Redis에서 캐비닛 상태 확인 - 빠른 경로 체크
        Object currentStatus = redisTemplate.opsForValue().get(CABINET_STATUS_KEY + cabinetId);
        if (currentStatus != null && !CabinetStatus.AVAILABLE.toString().equals(currentStatus.toString())) {
            // 명확한 CABINET_ALREADY_USING 예외 반환
            logger.info("Cabinet {} is already in use according to Redis status", cabinetId);
            return CompletableFuture.failedFuture(
                    new ServiceException(ExceptionStatus.CABINET_ALREADY_USING)
            );
        }

        // 2. 대여 가능 시간 체크
        if (cabinetRedisService.isBeforeReleaseTime(cabinetId)) {
            logger.info("Cabinet {} cannot be rented before release time", cabinetId);
            return CompletableFuture.failedFuture(
                    new ServiceException(ExceptionStatus.CABINET_RENT_FAILED)
            );
        }

        // 3. 프로세싱 중인지 확인
        if (cabinetRedisService.isProcessingRent(cabinetId)) {
            logger.info("Cabinet {} is currently being processed for rent", cabinetId);
            return CompletableFuture.failedFuture(
                    new ServiceException(ExceptionStatus.CABINET_RENT_FAILED)
            );
        }

        // 4. DB 최종 확인
        Optional<Cabinet> cabinetOpt = cabinetRepository.findById(cabinetId);
        if (cabinetOpt.isEmpty() ||
                cabinetOpt.get().getStatus() == CabinetStatus.USING ||
                cabinetOpt.get().getStatus() == CabinetStatus.OVERDUE) {
            logger.info("Cabinet {} is not available according to DB status", cabinetId);
            return CompletableFuture.failedFuture(
                    new ServiceException(ExceptionStatus.CABINET_ALREADY_USING)
            );
        }

        // 5. 락 획득 시도
        boolean canProceed = queueManager.addToQueueAndAcquireLock(cabinetId, studentNumber);
        if (!canProceed) {
            logger.info("Failed to acquire lock for cabinet {}", cabinetId);
            return CompletableFuture.failedFuture(
                    new ServiceException(ExceptionStatus.CABINET_RENT_FAILED)
            );
        }

        // 6. 임시 상태 설정
        cabinetRedisService.setTemporaryStatus(cabinetId, CabinetStatus.USING);

        // 나머지 처리...
        CompletableFuture<CabinetDetailVo> resultFuture = new CompletableFuture<>();
        setupResultListener(cabinetId, studentNumber, resultFuture);
        processRentRequestAsync(cabinetId, studentNumber);
        setupTimeout(cabinetId, studentNumber, resultFuture, 1);

        return resultFuture;
    }

    /**
     * 결과 리스너 설정
     */
    private void setupResultListener(Long cabinetId, String studentNumber, CompletableFuture<CabinetDetailVo> resultFuture) {
        // 메시지 리스너 생성
        MessageListener listener = (message, pattern) -> {
            try {
                String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
                String expectedValue = cabinetId + ":" + studentNumber;

                if (messageBody.equals(expectedValue) && !resultFuture.isDone()) {
                    String resultKey = CabinetRedisService.CABINET_OPERATION_RESULT_KEY + cabinetId + ":" + studentNumber;
                    Map<String, Object> resultMap = (Map<String, Object>) redisTemplate.opsForValue().get(resultKey);

                    if (resultMap != null) {
                        boolean success = (boolean) resultMap.getOrDefault("success", false);

                        if (!success) {
                            // 에러 케이스 - 구체적인 에러 메시지 전달
                            String errorMessage = (String) resultMap.getOrDefault("error", "대여 실패");
                            resultFuture.completeExceptionally(
                                    new ServiceException(ExceptionStatus.CABINET_RENT_FAILED)
                            );
                        } else {
                            // 성공 케이스
                            try {
                                // 캐비닛 정보 조회
                                Cabinet cabinet = cabinetRepository.findById(cabinetId).orElseThrow(
                                        () -> new ServiceException(ExceptionStatus.CABINET_NOT_FOUND)
                                );
                                Building building = cabinet.getBuildingId();

                                // 사용자 정보 조회
                                Authn authn = authnRepository.findByStudentNumber(studentNumber).orElseThrow(
                                        () -> new ServiceException(ExceptionStatus.USER_NOT_FOUND)
                                );
                                User user = authn.getUser();

                                // 응답 생성
                                CabinetDetailVo detailVo = new CabinetDetailVo(
                                        building.getFloor(),
                                        cabinet.getCabinetNumber().substring(0, 1),
                                        building.getName(),
                                        cabinet.getCabinetNumber(),
                                        cabinet.getStatus(),
                                        user.getIsVisible(),
                                        user.getName(),
                                        true,
                                        cabinet.getUpdatedAt()
                                );

                                resultFuture.complete(detailVo);
                            } catch (Exception e) {
                                resultFuture.completeExceptionally(e);
                            }
                        }

                        // 리스너 해제
                        cabinetRedisService.unregisterListener(cabinetId + ":" + studentNumber);
                    }
                }
            } catch (Exception e) {
                if (!resultFuture.isDone()) {
                    resultFuture.completeExceptionally(e);
                    cabinetRedisService.unregisterListener(cabinetId + ":" + studentNumber);
                }
            }
        };

        // 리스너 등록
        cabinetRedisService.registerListener(cabinetId + ":" + studentNumber, listener);
    }

    /**
     * 비동기적으로 Kafka 또는 폴백 서비스로 대여 요청 처리
     */
    private void processRentRequestAsync(Long cabinetId, String studentNumber) {
        CompletableFuture.runAsync(() -> {
            try {
                // 폴백 서비스 먼저 확인 - 더 빠른 처리를 위해
                if (cabinetFallbackService.shouldUseFallback()) {
                    try {
                        Optional<Cabinet> result = cabinetFallbackService.processRentRequest(
                                new CabinetKafkaDto.CabinetRentMessage(cabinetId, studentNumber, LocalDateTime.now())
                        );

                        if (result.isPresent()) {
                            // 즉시 결과 반환
                            cabinetRedisService.setOperationResult(cabinetId, studentNumber, true, null);
                            return;
                        }
                    } catch (Exception ignored) {
                        // 폴백 실패시 무시하고 Kafka로 계속 진행
                    }
                }

                // Kafka로 메시지 전송
                kafkaProducerService.sendRentRequest(cabinetId, studentNumber);
            } catch (Exception e) {
                // 실패 시 명시적으로 에러 설정
                cabinetRedisService.setOperationResult(cabinetId, studentNumber, false, e.getMessage());
            }
        }, cabinetTaskExecutor);
    }

    /**
     * 타임아웃 설정
     */
    private void setupTimeout(Long cabinetId, String studentNumber, CompletableFuture<CabinetDetailVo> resultFuture, int timeoutSeconds) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            if (!resultFuture.isDone()) {
                try {
                    // DB에서 최종 상태 확인
                    Optional<Cabinet> cabinetOpt = cabinetRepository.findById(cabinetId);

                    if (cabinetOpt.isPresent()) {
                        Cabinet cabinet = cabinetOpt.get();

                        // 실제로 대여됐는지 확인
                        if (cabinet.getStatus() == CabinetStatus.USING) {
                            // 성공 케이스 처리
                            Building building = cabinet.getBuildingId();
                            Optional<Authn> authnOpt = authnRepository.findByStudentNumber(studentNumber);

                            if (authnOpt.isPresent()) {
                                User user = authnOpt.get().getUser();

                                CabinetDetailVo detailVo = new CabinetDetailVo(
                                        building.getFloor(),
                                        cabinet.getCabinetNumber().substring(0, 1),
                                        building.getName(),
                                        cabinet.getCabinetNumber(),
                                        cabinet.getStatus(),
                                        user.getIsVisible(),
                                        user.getName(),
                                        true,
                                        cabinet.getUpdatedAt()
                                );

                                resultFuture.complete(detailVo);
                            } else {
                                resultFuture.completeExceptionally(
                                        new ServiceException(ExceptionStatus.USER_NOT_FOUND)
                                );
                            }
                        } else {
                            // 대여 실패
                            resultFuture.completeExceptionally(
                                    new ServiceException(ExceptionStatus.CABINET_RENT_FAILED)
                            );
                        }
                    } else {
                        resultFuture.completeExceptionally(
                                new ServiceException(ExceptionStatus.CABINET_NOT_FOUND)
                        );
                    }
                } catch (Exception e) {
                    resultFuture.completeExceptionally(
                            new ServiceException(ExceptionStatus.CABINET_RENT_FAILED)
                    );
                } finally {
                    // 리소스 정리
                    cabinetRedisService.unregisterListener(cabinetId + ":" + studentNumber);
                    cleanupResources(cabinetId, studentNumber);
                }
            }
            scheduler.shutdown();
        }, timeoutSeconds, TimeUnit.SECONDS);
    }
    /**
     * 리소스 정리를 위한 헬퍼 메소드
     */
    private void cleanupResources(Long cabinetId, String studentNumber) {
        // 임시 상태 복원
        cabinetRedisService.setTemporaryStatus(cabinetId, CabinetStatus.AVAILABLE);

        // 큐에서 제거
        queueManager.removeFromQueue(cabinetId, studentNumber);

        // 락 해제
        queueManager.releaseProcessingLock(cabinetId);

        // Redis 결과 키 삭제
        redisTemplate.delete(CabinetRedisService.CABINET_OPERATION_RESULT_KEY + cabinetId + ":" + studentNumber);
    }

    @Transactional
    public CabinetDetailVo returnCabinet(CabinetReturnVo requestVo) {
        Optional<Cabinet> cabinetOptional = cabinetRepository.returnCabinetByCabinetId(requestVo.cabinetId(), requestVo.studentNumber());

        if (cabinetOptional.isEmpty()) {
            throw new RuntimeException("캐비닛을 찾을 수 없습니다.");
        }
        Cabinet cabinet = cabinetOptional.get();
        Building building = cabinet.getBuildingId();
        User cabinetOwner = cabinet.getUserId();

        // 중요: Redis에서 캐비닛 상태를 마지막에 업데이트하여 덮어쓰기 방지
        cabinetRedisService.setTemporaryStatus(requestVo.cabinetId(), CabinetStatus.AVAILABLE);

        // 다음날 13시 대여 가능 시간 설정
        cabinetRedisService.setReleaseTime(requestVo.cabinetId());

        // 중요: 먼저 처리 중인 상태나 락 정보 삭제
        queueManager.releaseProcessingLock(requestVo.cabinetId());
        queueManager.releaseReturnLock(requestVo.cabinetId());





        // Redis에서 캐비닛 관련 모든 키 정리 (상태 키와 릴리즈 타임 키는 보존)
        cleanRedisKeysExceptStatusAndReleaseTime(requestVo.cabinetId(), requestVo.studentNumber());

        // 상태 키가 제대로 설정되었는지 확인 (즉시 검증)
        Object updatedStatus = redisTemplate.opsForValue().get(CABINET_STATUS_KEY + requestVo.cabinetId());
        if (updatedStatus == null || !updatedStatus.toString().equals(CabinetStatus.AVAILABLE.toString())) {
            // 상태가 없거나 일치하지 않으면 다시 설정
            cabinetRedisService.setTemporaryStatus(requestVo.cabinetId(), CabinetStatus.AVAILABLE);
            // 설정 후 바로 확인
            updatedStatus = redisTemplate.opsForValue().get(CABINET_STATUS_KEY + requestVo.cabinetId());
            logger.info("Cabinet status reset in Redis: cabinetId={}, status={}", requestVo.cabinetId(), updatedStatus);
        }

        Boolean isVisible = (cabinetOwner != null) ? cabinetOwner.getIsVisible() : true;

        return new CabinetDetailVo(
                building.getFloor(),
                cabinet.getCabinetNumber().substring(0, 1), // section
                building.getName(),
                cabinet.getCabinetNumber(),
                cabinet.getStatus(),
                isVisible,
                cabinetOwner != null ? cabinetOwner.getName() : null,
                false,
                cabinet.getUpdatedAt() // 만료일
        );
    }

    /**
     * 상태 키와 릴리즈 타임 키를 제외한 캐비닛 관련 Redis 키를 정리합니다.
     */
    private void cleanRedisKeysExceptStatusAndReleaseTime(Long cabinetId, String studentNumber) {
        try {
            // 대여 처리 키
            String rentProcessingKey = CABINET_RENT_PROCESSING_KEY + cabinetId;

            // 반납 처리 키
            String returnProcessingKey = CABINET_RETURN_PROCESSING_KEY + cabinetId;

            // 결과 키
            String resultKey = CABINET_OPERATION_RESULT_KEY + cabinetId + ":" + studentNumber;

            // Redis 키 삭제 (파이프라인 사용)
            redisTemplate.delete(List.of(rentProcessingKey, returnProcessingKey, resultKey));

            // 리스너 해제
            cabinetRedisService.unregisterListener(cabinetId + ":" + studentNumber);

            logger.info("Cleaned processing Redis keys for cabinet {} and student {}", cabinetId, studentNumber);
        } catch (Exception e) {
            logger.warn("Failed to clean Redis keys for cabinet {}: {}", cabinetId, e.getMessage());
        }
    }

    /**
     * 캐비닛 관련 Redis 키를 모두 정리합니다.
     */
    private void cleanRedisKeysForCabinet(Long cabinetId, String studentNumber) {
        try {
            // 대여 처리 키
            String rentProcessingKey = CABINET_RENT_PROCESSING_KEY + cabinetId;

            // 반납 처리 키
            String returnProcessingKey = CABINET_RETURN_PROCESSING_KEY + cabinetId;

            // 결과 키
            String resultKey = CABINET_OPERATION_RESULT_KEY + cabinetId + ":" + studentNumber;

            // Redis 키 삭제 (상태 키는 제외)
            redisTemplate.delete(List.of(rentProcessingKey, returnProcessingKey, resultKey));

            // 리스너 해제
            cabinetRedisService.unregisterListener(cabinetId + ":" + studentNumber);

            logger.info("Cleaned Redis keys for cabinet {} and student {} (status key preserved)", cabinetId, studentNumber);
        } catch (Exception e) {
            logger.warn("Failed to clean Redis keys for cabinet {}: {}", cabinetId, e.getMessage());
        }
    }

    public List<CabinetVo> searchCabinetByKeyword(CabinetSearchVo requestVo) {
        // 키워드로 캐비닛 검색
        Page<Cabinet> cabinetPage = cabinetRepository.searchCabinetsByKeyword(
                requestVo.keyword(),
                PageRequest.of(0, 5) // 기본 페이지네이션 설정
        );

        // 조회 결과가 없는 경우 빈 리스트 반환
        if (cabinetPage == null || cabinetPage.isEmpty()) {
            return new ArrayList<>();
        }

        // 현재 CabinetVo 구조에 맞게 변환
        return cabinetPage.getContent().stream()
                .map(cabinet -> new CabinetVo(
                        cabinet.getBuildingId().getName(),  // buildingName
                        cabinet.getBuildingId().getFloor(), // floor
                        cabinet.getCabinetNumber()          // cabinetNumber
                ))
                .collect(Collectors.toList());
    }

    public Page<CabinetVo> searchDetailByKeyword(CabinetSearchDetailVo requestVo) {
        // 페이징 정보 생성
        Pageable pageable = PageRequest.of(
                requestVo.page(),
                requestVo.pageSize()
        );

        // 키워드 검색 쿼리 실행
        Page<Cabinet> cabinetPage = cabinetRepository.findAllCabinetInfoByKeyword(
                requestVo.keyword(),
                pageable
        );

        // Page<Cabinet>을 Page<CabinetVo>로 변환
        return cabinetPage.map(cabinet -> new CabinetVo(
                cabinet.getBuildingId().getName(),
                cabinet.getBuildingId().getFloor(),
                cabinet.getCabinetNumber()
        ));
    }

    public Page<CabinetHistoryResponseVo> findCabinetRentHistory(CabinetHistoryVo requestVo) {
        // Create pageable with proper pagination
        Pageable pageable = PageRequest.of(
                requestVo.page(),  // Convert to 0-based indexing
                requestVo.pageSize()
        );

        // Fetch histories with pagination
        Page<CabinetHistory> historyPage = cabinetRepository
                .findCabinetHistoriesByStudentNumber(requestVo.studentNumber(), pageable);

        // Map entities to response VOs
        return historyPage.map(history -> {
            Cabinet cabinet = history.getCabinet();
            Building building = cabinet.getBuildingId();

            return new CabinetHistoryResponseVo(
                    building.getName(),
                    building.getFloor(),
                    building.getSection(),
                    cabinet.getCabinetNumber(),
                    history.getCreatedAt(),
                    history.getEndedAt()
            );
        });
    }


    // 사물함 상태를 page 객체에 담아 반환
    public Page<CabinetByStatusVo> findCabinetsByStatus(CabinetStatusVo statusVo, Pageable pageable){

        Page<Cabinet> page = cabinetRepository.findCabinetByStatus(statusVo.status(),pageable);

        return page.map(cabinet -> {
            CabinetPosition position = cabinetPositionRepository.findByCabinetId(cabinet)
                    .orElseThrow(() -> new ServiceException(ExceptionStatus.CABINET_POSITION_NOT_FOUND));

            User user = cabinet.getUserId();
            Optional<CabinetHistory> cabinetHistory =
                cabinetHistoryRepository.findTop1ByCabinetIdOrderByCreatedAtDesc(cabinet.getId());


            return new CabinetByStatusVo(
                    cabinet.getId(),
                    cabinet.getBuildingId().getName(),
                    cabinet.getBuildingId().getFloor(),
                    cabinet.getBuildingId().getSection(),
                    position,
                    cabinet.getCabinetNumber(),
                    cabinet.getStatus(),
                    user,
                    // reason - 상태가 AVAILABLE 이 아닌 경우 해당 STATUS 반환 향후 수정
                    cabinetHistory.isPresent() && cabinet.getStatus() != CabinetStatus.AVAILABLE? cabinet.getStatus().name(): null,
                    // rentalStartDate - USING 상태이고 히스토리가 있을 때만
                    cabinetHistory.isPresent() && cabinet.getStatus() == CabinetStatus.USING
                            ? cabinetHistory.get().getCreatedAt().toLocalDate()
                            : null,
                    // overDate - 히스토리가 있을 때만
                    cabinetHistory.isPresent()
                            ? cabinetHistory.get().getExpiredAt().toLocalDate()
                            : null,
                    // brokenDate - BROKEN 상태이고 히스토리가 있을 때만
                    cabinetHistory.isPresent() && cabinet.getStatus() == CabinetStatus.BROKEN
                            ? cabinetHistory.get().getUpdatedAt().toLocalDate()
                            : null
            );

        });

    }
}
