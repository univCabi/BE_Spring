package org.univcabi.univcabi.cabinet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.univcabi.univcabi.cabinet.dto.CabinetKafkaDto.CabinetRentMessage;
import org.univcabi.univcabi.cabinet.dto.CabinetKafkaDto.CabinetReturnMessage;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This service provides fallback functionality when Kafka is not available.
 * It directly processes the cabinet operations without going through Kafka.
 */
@Service
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers-optional", havingValue = "true")
public class CabinetFallbackService {

    private static final Logger logger = LoggerFactory.getLogger(CabinetFallbackService.class);

    private final CabinetRepository cabinetRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final boolean kafkaOptional;

    // Redis key constants
    private static final String CABINET_RENT_PROCESSING_KEY = "cabinet:processing:rent:";
    private static final String CABINET_RETURN_PROCESSING_KEY = "cabinet:processing:return:";

    public CabinetFallbackService(
            CabinetRepository cabinetRepository,
            RedisTemplate<String, Object> redisTemplate,
            @Value("${spring.kafka.bootstrap-servers-optional:false}") boolean kafkaOptional) {
        this.cabinetRepository = cabinetRepository;
        this.redisTemplate = redisTemplate;
        this.kafkaOptional = kafkaOptional;

        if (kafkaOptional) {
            logger.info("Kafka fallback service activated. Direct database operations will be used if Kafka is unavailable.");
        }
    }

    @PostConstruct
    public void init() {
        if (kafkaOptional) {
            // Clean up any stale processing locks at startup
            clearStaleProcessingLocks();
        }
    }

    /**
     * Process rent request directly when Kafka is not available
     */
    @Transactional
    public Optional<Cabinet> processRentRequest(CabinetRentMessage message) {
        Long cabinetId = message.cabinetId();
        String studentNumber = message.studentNumber();

        logger.info("Processing rent request via fallback for cabinet {} by student {}", cabinetId, studentNumber);

        try {
            return cabinetRepository.rentCabinetByCabinetId(cabinetId, studentNumber);
        } catch (Exception e) {
            logger.error("Fallback rent operation failed: {}", e.getMessage());
            throw e;
        } finally {
            // Always clean up the processing lock
            releaseRentProcessingLock(cabinetId);
        }
    }

    /**
     * Process return request directly when Kafka is not available
     */
    @Transactional
    public Optional<Cabinet> processReturnRequest(CabinetReturnMessage message) {
        Long cabinetId = message.cabinetId();
        String studentNumber = message.studentNumber();

        logger.info("Processing return request via fallback for cabinet {} by student {}", cabinetId, studentNumber);

        try {
            // First release the processing lock to prevent deadlock
            releaseReturnProcessingLock(cabinetId);

            // Then process the return operation
            Optional<Cabinet> result = cabinetRepository.returnCabinetByCabinetId(cabinetId, studentNumber);

            if (result.isPresent()) {
                logger.info("Successfully returned cabinet {} via fallback", cabinetId);
            } else {
                logger.warn("Failed to return cabinet {} via fallback", cabinetId);
            }

            return result;
        } catch (Exception e) {
            logger.error("Fallback return operation failed for cabinet {}: {}", cabinetId, e.getMessage());
            throw e;
        } finally {
            // Ensure the processing lock is released even if an exception occurs
            releaseReturnProcessingLock(cabinetId);
        }
    }

    /**
     * Check if Kafka fallback is enabled and should be used
     */
    public boolean shouldUseFallback() {
        return kafkaOptional;
    }

    /**
     * Release the rent processing lock for a cabinet
     */
    private void releaseRentProcessingLock(Long cabinetId) {
        try {
            String processingKey = CABINET_RENT_PROCESSING_KEY + cabinetId;
            redisTemplate.delete(processingKey);
            logger.debug("Released rent processing lock for cabinet {}", cabinetId);
        } catch (Exception e) {
            logger.warn("Failed to release rent processing lock: {}", e.getMessage());
        }
    }

    /**
     * Release the return processing lock for a cabinet
     */
    private void releaseReturnProcessingLock(Long cabinetId) {
        try {
            String processingKey = CABINET_RETURN_PROCESSING_KEY + cabinetId;
            redisTemplate.delete(processingKey);
            logger.debug("Released return processing lock for cabinet {}", cabinetId);
        } catch (Exception e) {
            logger.warn("Failed to release return processing lock: {}", e.getMessage());
        }
    }

    /**
     * Clear all stale processing locks
     */
    private void clearStaleProcessingLocks() {
        try {
            // Clear rent processing locks
            Set<String> rentLocks = redisTemplate.keys(CABINET_RENT_PROCESSING_KEY + "*");
            if (rentLocks != null && !rentLocks.isEmpty()) {
                redisTemplate.delete(rentLocks);
                logger.info("Cleared {} stale rent processing locks", rentLocks.size());
            }

            // Clear return processing locks
            Set<String> returnLocks = redisTemplate.keys(CABINET_RETURN_PROCESSING_KEY + "*");
            if (returnLocks != null && !returnLocks.isEmpty()) {
                redisTemplate.delete(returnLocks);
                logger.info("Cleared {} stale return processing locks", returnLocks.size());
            }
        } catch (Exception e) {
            logger.warn("Failed to clear stale processing locks: {}", e.getMessage());
        }
    }

    /**
     * Force clear all processing locks - can be called from an admin endpoint
     */
    public void forceClearAllLocks() {
        clearStaleProcessingLocks();
    }
}