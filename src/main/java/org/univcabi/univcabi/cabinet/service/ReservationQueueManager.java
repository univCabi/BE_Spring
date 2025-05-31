package org.univcabi.univcabi.cabinet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.univcabi.univcabi.cabinet.service.CabinetService.CABINET_RETURN_PROCESSING_KEY;

/**
 * Helper class to manage reservation queues for cabinets.
 * This ensures clean and consistent handling of reservation queues across the application.
 */
@Component
public class ReservationQueueManager {
    private static final Logger logger = LoggerFactory.getLogger(ReservationQueueManager.class);

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 키 상수
    private static final String CABINET_RESERVATION_QUEUE_KEY = "cabinet:reservation_queue:";
    private static final String CABINET_RENT_PROCESSING_KEY = "cabinet:processing:rent:";

    public ReservationQueueManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Add a student to the reservation queue for a cabinet.
     * @param cabinetId The cabinet ID
     * @param studentNumber The student number
     * @return true if the student is the first requester, false otherwise
     */
    public boolean addToQueue(Long cabinetId, String studentNumber) {
        String queueKey = CABINET_RESERVATION_QUEUE_KEY + cabinetId;

        try {
            // Add the student to the queue with current timestamp
            redisTemplate.opsForZSet().add(queueKey, studentNumber, System.currentTimeMillis());

            // Set expiration on the queue key if not exists (7 days)
            if (Boolean.FALSE.equals(redisTemplate.hasKey(queueKey))) {
                redisTemplate.expire(queueKey, 1, TimeUnit.HOURS);
            }

            // Check if the student is the first requester
            Set<Object> firstRequester = redisTemplate.opsForZSet().range(queueKey, 0, 0);
            if (firstRequester == null || firstRequester.isEmpty()) {
                return true; // Empty queue, this is the first request
            }

            return firstRequester.iterator().next().equals(studentNumber);
        } catch (Exception e) {
            logger.warn("Error processing reservation queue: {}", e.getMessage());
            return true; // On error, allow the operation to proceed
        }
    }

    /**
     * Remove a student from the reservation queue.
     * @param cabinetId The cabinet ID
     * @param studentNumber The student number to remove
     */
    public void removeFromQueue(Long cabinetId, String studentNumber) {
        String queueKey = CABINET_RESERVATION_QUEUE_KEY + cabinetId;

        try {
            redisTemplate.opsForZSet().remove(queueKey, studentNumber);
        } catch (Exception e) {
            logger.warn("Failed to remove from reservation queue: {}", e.getMessage());
        }
    }

    /**
     * Clear the entire reservation queue for a cabinet.
     * @param cabinetId The cabinet ID
     */
    public void clearQueue(Long cabinetId) {
        String queueKey = CABINET_RESERVATION_QUEUE_KEY + cabinetId;

        try {
            redisTemplate.delete(queueKey);
        } catch (Exception e) {
            logger.warn("Failed to clear reservation queue: {}", e.getMessage());
        }
    }

    /**
     * Acquire a processing lock for a cabinet rental operation.
     * @param cabinetId The cabinet ID
     * @param studentNumber The student number
     * @return true if the lock was acquired, false otherwise
     */
    public boolean acquireProcessingLock(Long cabinetId, String studentNumber) {
        String processingKey = CABINET_RENT_PROCESSING_KEY + cabinetId;

        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                    processingKey,
                    studentNumber,
                    30, // 30초 TTL
                    TimeUnit.SECONDS
            ));
        } catch (Exception e) {
            logger.warn("Failed to acquire processing lock: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Release a processing lock for a cabinet.
     * @param cabinetId The cabinet ID
     */
    public void releaseProcessingLock(Long cabinetId) {
        String processingKey = CABINET_RENT_PROCESSING_KEY + cabinetId;

        try {
            redisTemplate.delete(processingKey);
        } catch (Exception e) {
            logger.warn("Failed to release processing lock: {}", e.getMessage());
        }
    }

    /**
     * Check if a student is the first in the queue.
     * @param cabinetId The cabinet ID
     * @param studentNumber The student number
     * @return true if the student is first in queue, false otherwise
     */
    public boolean isFirstInQueue(Long cabinetId, String studentNumber) {
        String queueKey = CABINET_RESERVATION_QUEUE_KEY + cabinetId;

        try {
            Set<Object> firstRequester = redisTemplate.opsForZSet().range(queueKey, 0, 0);
            if (firstRequester == null || firstRequester.isEmpty()) {
                return true; // Empty queue
            }

            return firstRequester.iterator().next().equals(studentNumber);
        } catch (Exception e) {
            logger.warn("Failed to check queue position: {}", e.getMessage());
            return true; // On error, allow the operation
        }
    }

    /**
     * Get the queue size for a cabinet.
     * @param cabinetId The cabinet ID
     * @return The number of students in the queue
     */
    public long getQueueSize(Long cabinetId) {
        String queueKey = CABINET_RESERVATION_QUEUE_KEY + cabinetId;

        try {
            Long size = redisTemplate.opsForZSet().size(queueKey);
            return size != null ? size : 0;
        } catch (Exception e) {
            logger.warn("Failed to get queue size: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Acquire a processing lock for a cabinet return operation.
     * If the same student already has the lock, it will be considered as a duplicate request.
     *
     * @param cabinetId The cabinet ID
     * @param studentNumber The student number
     * @return true if the lock was acquired or is already owned by the same student, false otherwise
     */
    public boolean acquireReturnLock(Long cabinetId, String studentNumber) {
        String processingKey = CABINET_RETURN_PROCESSING_KEY + cabinetId;

        try {
            // Check if the lock already exists
            String existingLockOwner = (String) redisTemplate.opsForValue().get(processingKey);
            if (existingLockOwner != null) {
                // If the same student already has the lock, allow the operation (idempotent)
                if (existingLockOwner.equals(studentNumber)) {
                    logger.info("Student {} already has the return lock for cabinet {}, allowing duplicate request",
                            studentNumber, cabinetId);
                    return true;
                }
                logger.warn("Cabinet {} return lock already held by {}, denying request from {}",
                        cabinetId, existingLockOwner, studentNumber);
                return false;
            }

            // Try to acquire a new lock
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    processingKey,
                    studentNumber,
                    30, // 30초 TTL
                    TimeUnit.SECONDS
            );

            if (Boolean.TRUE.equals(acquired)) {
                logger.debug("Student {} acquired return lock for cabinet {}", studentNumber, cabinetId);
                return true;
            }

            logger.warn("Failed to acquire return lock for cabinet {} by student {}", cabinetId, studentNumber);
            return false;
        } catch (Exception e) {
            logger.error("Error acquiring return lock for cabinet {}: {}", cabinetId, e.getMessage());
            // In case of Redis error, allow the operation rather than blocking it
            return true;
        }
    }

    /**
     * Release a processing lock for a cabinet return operation.
     * @param cabinetId The cabinet ID
     */
    public void releaseReturnLock(Long cabinetId) {
        String processingKey = CABINET_RETURN_PROCESSING_KEY + cabinetId;

        try {
            redisTemplate.delete(processingKey);
            logger.debug("Released return lock for cabinet {}", cabinetId);
        } catch (Exception e) {
            logger.warn("Failed to release return lock for cabinet {}: {}", cabinetId, e.getMessage());
        }
    }

    /**
     * Check if a cabinet is already being processed for return
     * @param cabinetId The cabinet ID
     * @return true if the cabinet is being processed, false otherwise
     */
    public boolean isReturnInProgress(Long cabinetId) {
        String processingKey = CABINET_RETURN_PROCESSING_KEY + cabinetId;

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(processingKey));
        } catch (Exception e) {
            logger.warn("Failed to check return progress for cabinet {}: {}", cabinetId, e.getMessage());
            return false;
        }
    }

    /**
     * Get the student number who currently has the return lock
     * @param cabinetId The cabinet ID
     * @return The student number or null if no lock exists
     */
    public String getReturnLockOwner(Long cabinetId) {
        String processingKey = CABINET_RETURN_PROCESSING_KEY + cabinetId;

        try {
            return (String) redisTemplate.opsForValue().get(processingKey);
        } catch (Exception e) {
            logger.warn("Failed to get return lock owner for cabinet {}: {}", cabinetId, e.getMessage());
            return null;
        }
    }

    /**
     * Check if the reservation queue for a cabinet is empty.
     * @param cabinetId The cabinet ID
     * @return true if the queue is empty, false otherwise
     */
    public boolean isQueueEmpty(Long cabinetId) {
        String queueKey = CABINET_RESERVATION_QUEUE_KEY + cabinetId;

        try {
            Long size = redisTemplate.opsForZSet().size(queueKey);
            return size == null || size == 0;
        } catch (Exception e) {
            logger.warn("Failed to check if queue is empty for cabinet {}: {}", cabinetId, e.getMessage());
            return true; // 오류 발생 시 빈 큐로 간주 (안전한 처리)
        }
    }

    /**
     * 큐에 추가 및 락 획득을 한 번에 처리 (원자적 작업)
     */
    public boolean addToQueueAndAcquireLock(Long cabinetId, String studentNumber) {
        String queueKey = CABINET_RESERVATION_QUEUE_KEY + cabinetId;
        String lockKey = CABINET_RENT_PROCESSING_KEY + cabinetId;

        // Redis 트랜잭션으로 원자적 처리
        return redisTemplate.execute(new SessionCallback<Boolean>() {
            @Override
            @SuppressWarnings("unchecked")
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.watch(lockKey); // 락 키 감시 시작

                // 이미 락이 있는지 확인
                Boolean lockExists = operations.hasKey(lockKey);
                if (lockExists != null && lockExists) {
                    // 이미 처리 중이면 실패
                    return false;
                }

                // 트랜잭션 시작
                operations.multi();

                // 큐에 추가
                operations.opsForList().leftPush(queueKey, studentNumber);

                // 락 설정 (30초 만료)
                operations.opsForValue().set(lockKey, studentNumber, 30, TimeUnit.SECONDS);

                // 트랜잭션 실행
                List<Object> results = operations.exec();

                // exec이 null이거나 빈 리스트면 트랜잭션 실패 (다른 스레드가 변경함)
                return results != null && !results.isEmpty();
            }
        });
    }
}