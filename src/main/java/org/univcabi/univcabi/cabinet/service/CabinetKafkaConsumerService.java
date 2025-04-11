package org.univcabi.univcabi.cabinet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.cabinet.dto.CabinetKafkaDto.CabinetRentMessage;
import org.univcabi.univcabi.cabinet.dto.CabinetKafkaDto.CabinetReturnMessage;
import org.univcabi.univcabi.cabinet.dto.CabinetKafkaDto.CabinetStatusUpdateMessage;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.RepositoryException;
import org.univcabi.univcabi.exception.ServiceException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CabinetKafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(CabinetKafkaConsumerService.class);

    private final CabinetRepository cabinetRepository;
    private final CabinetKafkaProducerService producerService;
    private final ObjectMapper objectMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    public CabinetKafkaConsumerService(
            CabinetRepository cabinetRepository,
            CabinetKafkaProducerService producerService,
            RedisTemplate<String, Object> redisTemplate  ) {
        this.cabinetRepository = cabinetRepository;
        this.producerService = producerService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "cabinet-rent-requests", groupId = "locker-group")
    public void consumeRentRequest(String message) {
        try {
            CabinetRentMessage rentMessage = objectMapper.readValue(message, CabinetRentMessage.class);

            try {
                Optional<Cabinet> result = cabinetRepository.rentCabinetByCabinetId(
                        rentMessage.cabinetId(),
                        rentMessage.studentNumber()
                );

                if (result.isPresent()) {
                    // Success case
                    producerService.sendStatusUpdate(new CabinetStatusUpdateMessage(
                            rentMessage.cabinetId(),
                            CabinetStatus.USING,
                            rentMessage.studentNumber(),
                            LocalDateTime.now(),
                            true,
                            null
                    ));
                } else {
                    // Failure case
                    producerService.sendStatusUpdate(new CabinetStatusUpdateMessage(
                            rentMessage.cabinetId(),
                            null,
                            rentMessage.studentNumber(),
                            LocalDateTime.now(),
                            false,
                            "Failed to rent cabinet"
                    ));
                }
            } catch (RepositoryException e) {
                // Handle specific error cases
                producerService.sendStatusUpdate(new CabinetStatusUpdateMessage(
                        rentMessage.cabinetId(),
                        null,
                        rentMessage.studentNumber(),
                        LocalDateTime.now(),
                        false,
                        e.getMessage()
                ));
            } catch (Exception e) {
                // Handle unexpected errors
                producerService.sendStatusUpdate(new CabinetStatusUpdateMessage(
                        rentMessage.cabinetId(),
                        null,
                        rentMessage.studentNumber(),
                        LocalDateTime.now(),
                        false,
                        "Unexpected error: " + e.getMessage()
                ));
            }
        } catch (JsonProcessingException e) {
            // Log deserialization errors
            System.err.println("Failed to parse rent request message: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "cabinet-return-requests", groupId = "locker-group")
    public void consumeReturnRequest(String message) {
        try {
            CabinetReturnMessage returnMessage = objectMapper.readValue(message, CabinetReturnMessage.class);

            try {
                Optional<Cabinet> result = cabinetRepository.returnCabinetByCabinetId(
                        returnMessage.cabinetId(),
                        returnMessage.studentNumber()
                );

                if (result.isPresent()) {
                    // Success case
                    producerService.sendStatusUpdate(new CabinetStatusUpdateMessage(
                            returnMessage.cabinetId(),
                            CabinetStatus.AVAILABLE,
                            returnMessage.studentNumber(),
                            LocalDateTime.now(),
                            true,
                            null
                    ));
                } else {
                    // Failure case
                    producerService.sendStatusUpdate(new CabinetStatusUpdateMessage(
                            returnMessage.cabinetId(),
                            null,
                            returnMessage.studentNumber(),
                            LocalDateTime.now(),
                            false,
                            "Failed to return cabinet"
                    ));
                }
            } catch (RepositoryException e) {
                // Handle specific error cases
                producerService.sendStatusUpdate(new CabinetStatusUpdateMessage(
                        returnMessage.cabinetId(),
                        null,
                        returnMessage.studentNumber(),
                        LocalDateTime.now(),
                        false,
                        e.getMessage()
                ));
            } catch (Exception e) {
                // Handle unexpected errors
                producerService.sendStatusUpdate(new CabinetStatusUpdateMessage(
                        returnMessage.cabinetId(),
                        null,
                        returnMessage.studentNumber(),
                        LocalDateTime.now(),
                        false,
                        "Unexpected error: " + e.getMessage()
                ));
            }
        } catch (JsonProcessingException e) {
            // Log deserialization errors
            System.err.println("Failed to parse return request message: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "cabinet-status-updates", groupId = "locker-group")
    public void consumeStatusUpdates(String message) {
        try {
            CabinetStatusUpdateMessage statusMessage = objectMapper.readValue(message, CabinetStatusUpdateMessage.class);
            Long cabinetId = statusMessage.cabinetId();
            String studentNumber = statusMessage.studentNumber();

            // Redis에 상태 업데이트
            String statusKey = CabinetService.CABINET_STATUS_KEY + cabinetId;

            if (statusMessage.success()) {
                // 성공 케이스: 상태 업데이트
                if (statusMessage.status() != null) {
                    redisTemplate.opsForValue().set(
                            statusKey,
                            statusMessage.status().toString(),
                            5,
                            TimeUnit.MINUTES
                    );
                }

                // 결과 저장: Map 객체로 저장
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("success", true);

                redisTemplate.opsForValue().set(
                        CabinetService.CABINET_OPERATION_RESULT_KEY + cabinetId + ":" + studentNumber,
                        resultMap,
                        30,
                        TimeUnit.SECONDS
                );
            } else {
                // 실패 케이스: 상태 복구
                redisTemplate.opsForValue().set(
                        statusKey,
                        CabinetStatus.AVAILABLE.toString(),
                        5,
                        TimeUnit.MINUTES
                );

                // 결과 저장: Map 객체로 저장
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("success", false);
                resultMap.put("error", statusMessage.errorMessage());

                redisTemplate.opsForValue().set(
                        CabinetService.CABINET_OPERATION_RESULT_KEY + cabinetId + ":" + studentNumber,
                        resultMap,
                        30,
                        TimeUnit.SECONDS
                );
            }

            // 이벤트 발행
            redisTemplate.convertAndSend(
                    CabinetService.CABINET_OPERATION_CHANNEL,
                    cabinetId + ":" + studentNumber
            );

            // 로깅
            if (statusMessage.success()) {
                logger.info("Cabinet {} status updated to {} for student {}",
                        cabinetId, statusMessage.status(), studentNumber);
            } else {
                logger.warn("Failed to update cabinet {} status: {}",
                        cabinetId, statusMessage.errorMessage());
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse status update message: {}", e.getMessage());
        }
    }
}