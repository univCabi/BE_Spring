package org.univcabi.univcabi.cabinet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.cabinet.dto.CabinetKafkaDto.CabinetRentMessage;
import org.univcabi.univcabi.cabinet.dto.CabinetKafkaDto.CabinetReturnMessage;
import org.univcabi.univcabi.cabinet.dto.CabinetKafkaDto.CabinetStatusUpdateMessage;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.configs.KafkaTopicConfig;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.ServiceException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class CabinetKafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(CabinetKafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final CabinetFallbackService fallbackService;

    public CabinetKafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate,
            CabinetFallbackService fallbackService) {
        this.kafkaTemplate = kafkaTemplate;
        this.fallbackService = fallbackService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public Optional<Cabinet> sendRentRequest(Long cabinetId, String studentNumber) {
        CabinetRentMessage message = new CabinetRentMessage(
                cabinetId,
                studentNumber,
                LocalDateTime.now()
        );

        try {
            String payload = objectMapper.writeValueAsString(message);

            try {
                CompletableFuture<SendResult<String, String>> future =
                        kafkaTemplate.send(KafkaTopicConfig.RENT_TOPIC, String.valueOf(cabinetId), payload);

                // Wait for a short time to confirm message was sent
                future.get(2, TimeUnit.SECONDS);
                logger.info("Rent request sent to Kafka for cabinet {}", cabinetId);
                return Optional.empty(); // Message was sent successfully, no Cabinet to return yet
            } catch (ExecutionException | InterruptedException | TimeoutException | KafkaException e) {
                // Kafka is not available, use fallback if enabled
                logger.warn("Failed to send rent request to Kafka: {}. Will try fallback.", e.getMessage());

                if (fallbackService.shouldUseFallback()) {
                    logger.info("Using fallback for rent operation on cabinet {}", cabinetId);
                    return fallbackService.processRentRequest(message);
                } else {
                    throw new ServiceException(ExceptionStatus.GENERAL_SERVICE_UNAVAILABLE);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize rent request message", e);
            throw new ServiceException(ExceptionStatus.CABINET_RENT_FAILED);
        }
    }

    public void sendStatusUpdate(CabinetStatusUpdateMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);

            try {
                kafkaTemplate.send(KafkaTopicConfig.STATUS_UPDATE_TOPIC,
                        String.valueOf(message.cabinetId()), payload);
                logger.info("Status update sent to Kafka for cabinet {}: {}",
                        message.cabinetId(), message.status());
            } catch (Exception e) {
                // Kafka 전송 실패
                logger.error("Failed to send status update to Kafka: {}", e.getMessage());

                if (e instanceof KafkaException) {
                    throw new ServiceException(ExceptionStatus.GENERAL_SERVICE_UNAVAILABLE);
                } else {
                    throw new ServiceException(ExceptionStatus.CABINET_RENT_FAILED);
                }
            }
        } catch (JsonProcessingException e) {
            // JSON 변환 실패 (잘못된 메시지 형식)
            logger.error("Failed to serialize status update message: {}", e.getMessage());
            throw new ServiceException(ExceptionStatus.GENERAL_BAD_REQUEST);
        }
    }
}