package org.univcabi.univcabi.cabinet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.ServiceException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class CabinetRedisService {

    private static final Logger logger = LoggerFactory.getLogger(CabinetRedisService.class);

    public static final String CABINET_RENT_PROCESSING_KEY = "cabinet:processing:rent:";
    public static final String CABINET_RETURN_PROCESSING_KEY = "cabinet:processing:return:";
    public static final String CABINET_STATUS_KEY = "cabinet:status:";
    public static final String CABINET_AVAILABLE_KEY = "cabinet:available:";
    public static final String CABINET_RESERVATION_QUEUE_KEY = "cabinet:reservation_queue:";
    public static final String CABINET_RELEASE_TIME_KEY = "cabinet:release_time:";
    public static final String CABINET_OPERATION_RESULT_KEY = "cabinet:operation:result:";
    public static final String CABINET_OPERATION_CHANNEL = "cabinet:operation:channel";

    private final RedisTemplate<String, Object> redisTemplate;
    private final CabinetRepository cabinetRepository;
    private final RedisMessageListenerContainer redisMessageListenerContainer;

    // 메시지 리스너를 추적하기 위한 맵
    private final ConcurrentHashMap<String, MessageListener> activeListeners = new ConcurrentHashMap<>();

    public CabinetRedisService(
            RedisTemplate<String, Object> redisTemplate,
            CabinetRepository cabinetRepository,
            RedisMessageListenerContainer redisMessageListenerContainer
    ) {
        this.redisTemplate = redisTemplate;
        this.cabinetRepository = cabinetRepository;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    /**
     * Clear all processing locks at startup
     */
    public void clearAllProcessingLocks() {
        try {
            clearReturnLocks();
            clearRentLocks();
            clearOperationResults();
        } catch (Exception e) {
            logger.warn("Failed to clear stale locks at startup: {}", e.getMessage());
        }
    }

    private void clearReturnLocks() {
        Set<String> returnLocks = redisTemplate.keys(CABINET_RETURN_PROCESSING_KEY + "*");
        if (returnLocks != null && !returnLocks.isEmpty()) {
            redisTemplate.delete(returnLocks);
            logger.info("Cleared {} stale cabinet return locks at startup", returnLocks.size());
        }
    }

    private void clearRentLocks() {
        Set<String> rentLocks = redisTemplate.keys(CABINET_RENT_PROCESSING_KEY + "*");
        if (rentLocks != null && !rentLocks.isEmpty()) {
            redisTemplate.delete(rentLocks);
            logger.info("Cleared {} stale cabinet rent locks at startup", rentLocks.size());
        }
    }

    private void clearOperationResults() {
        Set<String> resultKeys = redisTemplate.keys(CABINET_OPERATION_RESULT_KEY + "*");
        if (resultKeys != null && !resultKeys.isEmpty()) {
            redisTemplate.delete(resultKeys);
            logger.info("Cleared {} stale operation results at startup", resultKeys.size());
        }
    }

    /**
     * 캐비닛이 대여 가능한지 빠르게 확인
     */
    public boolean canRentCabinet(Long cabinetId) {
        try {
            // 1. 프로세싱 락 확인
            Boolean hasLock = redisTemplate.hasKey(CABINET_RENT_PROCESSING_KEY + cabinetId);
            if (hasLock != null && hasLock) {
                return false; // 이미 처리 중
            }
            // 1. 기존 상태 체크 로직
            List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                byte[] statusKeyBytes = (CABINET_STATUS_KEY + cabinetId).getBytes(StandardCharsets.UTF_8);
                byte[] processingKeyBytes = (CABINET_RENT_PROCESSING_KEY + cabinetId).getBytes(StandardCharsets.UTF_8);
                byte[] returnProcessingKeyBytes = (CABINET_RETURN_PROCESSING_KEY + cabinetId).getBytes(StandardCharsets.UTF_8);
                byte[] releaseTimeKeyBytes = (CABINET_RELEASE_TIME_KEY + cabinetId).getBytes(StandardCharsets.UTF_8);

                // 1. 상태 확인
                connection.stringCommands().get(statusKeyBytes);

                // 2. 대여 처리 중인지 확인
                connection.keyCommands().exists(processingKeyBytes);

                // 3. 반납 처리 중인지 확인
                connection.keyCommands().exists(returnProcessingKeyBytes);

                // 4. 대여 가능 시간 확인
                connection.stringCommands().get(releaseTimeKeyBytes);

                return null;
            });

            // 기존 상태 체크 로직
            String statusStr = (String) results.get(0);
            if (statusStr != null) {
                CabinetStatus status;
                try {
                    status = CabinetStatus.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid cabinet status string in Redis: {}", statusStr);
                    return false;
                }

                if (status == CabinetStatus.USING || status == CabinetStatus.OVERDUE) {
                    logger.debug("Cabinet {} is already in use or overdue", cabinetId);
                    return false;
                }
            }

            Boolean isRentProcessing = (Boolean) results.get(1);
            if (Boolean.TRUE.equals(isRentProcessing)) {
                logger.debug("Cabinet {} is already being processed for rent", cabinetId);
                return false;
            }

            Boolean isReturnProcessing = (Boolean) results.get(2);
            if (Boolean.TRUE.equals(isReturnProcessing)) {
                logger.debug("Cabinet {} is being processed for return", cabinetId);
                return false;
            }

            // 추가: 대여 가능 시간 체크
            String releaseTimeStr = (String) results.get(3);
            if (releaseTimeStr != null) {
                LocalDateTime releaseTime = LocalDateTime.parse(releaseTimeStr);
                LocalDateTime now = LocalDateTime.now();

                if (now.isBefore(releaseTime)) {
                    logger.debug("Cabinet {} is not available until {}", cabinetId, releaseTime);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Error checking if cabinet can be rented: {}", e.getMessage());
            throw new ServiceException(ExceptionStatus.GENERAL_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 반납 후 다음날 13시 시간 설정
     */
    public void setReleaseTime(Long cabinetId) {
        try {
            // 다음 날 13시 시간 계산
            LocalDateTime releaseTime = LocalDateTime.of(
                    LocalDate.now().plusDays(1),
                    LocalTime.of(13, 0)
            );

            String releaseTimeKey = CABINET_RELEASE_TIME_KEY + cabinetId;

            // 7일 만료시간으로 설정 (충분히 긴 시간)
            redisTemplate.opsForValue().set(
                    releaseTimeKey,
                    releaseTime.toString(),
                    7,
                    TimeUnit.DAYS
            );

            logger.debug("Set release time for cabinet {} to {}", cabinetId, releaseTime);
        } catch (Exception e) {
            logger.error("Failed to set release time: {}", e.getMessage());
        }
    }

    /**
     * 대여 가능 시간 확인
     */
    public boolean isBeforeReleaseTime(Long cabinetId) {
        String releaseTimeKey = CABINET_RELEASE_TIME_KEY + cabinetId;
        String releaseTimeStr = (String) redisTemplate.opsForValue().get(releaseTimeKey);

        if (releaseTimeStr != null) {
            try {
                LocalDateTime releaseTime = LocalDateTime.parse(releaseTimeStr);
                LocalDateTime now = LocalDateTime.now();

                return now.isBefore(releaseTime);
            } catch (Exception e) {
                logger.error("Error parsing release time: {}", e.getMessage());
            }
        }

        // 시간 정보가 없으면 대여 가능
        return false;
    }

    /**
     * 작업 결과 설정 (JSON 직렬화 오류 해결)
     */
    public void setOperationResult(Long cabinetId, String studentNumber, boolean success, String errorMessage) {
        try {
            String resultKey = CABINET_OPERATION_RESULT_KEY + cabinetId + ":" + studentNumber;

            // JSON 객체로 결과 저장 (문자열 대신)
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("success", success);
            if (!success && errorMessage != null) {
                resultMap.put("error", errorMessage);
            }

            // 결과 저장 및 이벤트 발행을 파이프라인으로 처리
            redisTemplate.opsForValue().set(resultKey, resultMap, 30, TimeUnit.SECONDS);

            // 이벤트 발행 (별도 호출로 분리)
            redisTemplate.convertAndSend(
                    CABINET_OPERATION_CHANNEL,
                    cabinetId + ":" + studentNumber
            );

            logger.debug("Set operation result for cabinet {}: {}", cabinetId, success ? "SUCCESS" : "ERROR");
        } catch (Exception e) {
            logger.error("Failed to set operation result: {}", e.getMessage());
        }
    }
    /**
     * 캐비닛이 대여 처리 중인지 확인
     */
    public boolean isProcessingRent(Long cabinetId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CABINET_RENT_PROCESSING_KEY + cabinetId));
    }

    /**
     * Redis에서 캐비닛 상태 조회
     */
    public CabinetStatus getCabinetStatus(Long cabinetId) {
        String statusKey = CABINET_STATUS_KEY + cabinetId;
        Object statusObj = redisTemplate.opsForValue().get(statusKey);

        if (statusObj == null) {
            // Redis에 없으면 DB에서 조회
            Optional<Cabinet> cabinet = cabinetRepository.findById(cabinetId);
            return cabinet.map(Cabinet::getStatus).orElse(CabinetStatus.AVAILABLE);
        }

        try {
            if (statusObj instanceof String) {
                return CabinetStatus.valueOf((String) statusObj);
            } else if (statusObj instanceof CabinetStatus) {
                return (CabinetStatus) statusObj;
            } else {
                logger.warn("Unexpected status type for cabinet {}: {}", cabinetId, statusObj.getClass());
                return CabinetStatus.AVAILABLE;
            }
        } catch (Exception e) {
            logger.error("Error parsing cabinet status: {}", e.getMessage());
            return CabinetStatus.AVAILABLE;
        }
    }

    /**
     * Set temporary cabinet status in Redis (for UI immediate feedback)
     */
    public void setTemporaryStatus(Long cabinetId, CabinetStatus status) {
        String statusKey = CABINET_STATUS_KEY + cabinetId;

        // 1. 상태 설정을 확실하게 동기적으로 처리
        redisTemplate.opsForValue().set(statusKey, status.toString());

        // 2. 설정 후 검증 (디버깅 용도)
        Object savedStatus = redisTemplate.opsForValue().get(statusKey);
        logger.info("Cabinet {} status set to {} in Redis. Verification: {}",
                cabinetId, status, savedStatus);

        // 3. 상태가 제대로 설정되지 않았으면 다시 시도
        if (savedStatus == null || !savedStatus.toString().equals(status.toString())) {
            logger.warn("Failed to set status for cabinet {} to {}. Retrying...", cabinetId, status);
            redisTemplate.opsForValue().set(statusKey, status.toString());
        }
    }


    /**
     * Redis Pub/Sub 리스너 등록 및 CompletableFuture 반환
     */
    public <T> void listenForOperationResult(
            Long cabinetId,
            String studentNumber,
            CompletableFuture<T> resultFuture,
            OperationResultHandler<T> resultHandler) {

        String listenerId = cabinetId + ":" + studentNumber;

        // 이미 완료된 future에 대해서는 리스너 등록 안함
        if (resultFuture.isDone()) {
            return;
        }

        // 먼저 기존에 처리된 결과가 있는지 확인 (동시성 개선)
        String resultKey = CABINET_OPERATION_RESULT_KEY + cabinetId + ":" + studentNumber;
        String existingResult = (String) redisTemplate.opsForValue().get(resultKey);

        if (existingResult != null) {
            // 이미 결과가 있으면 즉시 처리
            handleResult(existingResult, resultHandler, resultFuture);
            return;
        }

        MessageListener listener = (message, pattern) -> {
            try {
                // 이미 완료된 future에 대해서는 처리 스킵
                if (resultFuture.isDone()) {
                    unregisterListener(listenerId);
                    return;
                }

                String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
                String expectedValue = cabinetId + ":" + studentNumber;

                // 해당 캐비닛과 학생에 대한 이벤트인지 확인
                if (messageBody.equals(expectedValue)) {
                    // 결과 조회
                    String result = (String) redisTemplate.opsForValue().get(resultKey);

                    if (result != null) {
                        handleResult(result, resultHandler, resultFuture);
                    }

                    // 메시지 처리 후 리스너 해제
                    unregisterListener(listenerId);
                }
            } catch (Exception e) {
                if (!resultFuture.isDone()) {
                    resultFuture.completeExceptionally(e);
                }

                // 리스너 해제
                unregisterListener(listenerId);
            }
        };

        // 리스너 등록
        registerListener(listenerId, listener);
    }

    private <T> void handleResult(String result, OperationResultHandler<T> resultHandler,
                                  CompletableFuture<T> resultFuture) {
        if (result.startsWith("ERROR:")) {
            // 에러 케이스
            String errorMessage = result.substring(6); // "ERROR:" 이후
            resultFuture.completeExceptionally(
                    new ServiceException(ExceptionStatus.CABINET_RENT_FAILED)
            );
        } else {
            // 성공 케이스
            try {
                // 결과 핸들러 호출
                T resultValue = resultHandler.handleSuccess();
                resultFuture.complete(resultValue);
            } catch (Exception e) {
                resultFuture.completeExceptionally(e);
            }
        }
    }

    /**
     * 리스너 등록
     */
    public void registerListener(String listenerId, MessageListener listener) {
        // 기존 리스너 해제
        unregisterListener(listenerId);

        // 새 리스너 등록
        redisMessageListenerContainer.addMessageListener(
                listener,
                new ChannelTopic(CABINET_OPERATION_CHANNEL)
        );

        // 맵에 저장
        activeListeners.put(listenerId, listener);

        logger.debug("Registered listener for {}", listenerId);
    }

    /**
     * 리스너 해제
     */
    public void unregisterListener(String listenerId) {
        MessageListener listener = activeListeners.remove(listenerId);
        if (listener != null) {
            redisMessageListenerContainer.removeMessageListener(listener);
            logger.debug("Unregistered listener for {}", listenerId);
        }
    }

    /**
     * 결과 핸들러 인터페이스 (람다 사용 가능)
     */
    @FunctionalInterface
    public interface OperationResultHandler<T> {
        T handleSuccess() throws Exception;
    }
}