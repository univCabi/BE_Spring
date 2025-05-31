package org.univcabi.univcabi.configs;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This component periodically checks Kafka's health status
 * and provides a method to check if Kafka is available.
 */
@Component
public class KafkaHealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(KafkaHealthChecker.class);

    private final KafkaAdmin kafkaAdmin;
    private final AtomicBoolean kafkaAvailable = new AtomicBoolean(false);

    @Value("${spring.kafka.bootstrap-servers-optional:false}")
    private boolean kafkaOptional;

    public KafkaHealthChecker(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
        // Check Kafka availability at startup
        checkKafkaAvailability();
    }

    /**
     * Check Kafka availability every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void checkKafkaAvailability() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterOptions options = new DescribeClusterOptions().timeoutMs(5000);
            DescribeClusterResult result = adminClient.describeCluster(options);

            // Try to get cluster info with timeout
            result.nodes().get(5, TimeUnit.SECONDS);

            // If we reach here, Kafka is available
            boolean wasAvailable = kafkaAvailable.getAndSet(true);
            if (!wasAvailable) {
                logger.info("Kafka connection established. Kafka is now available.");
            }
        } catch (Exception e) {
            boolean wasAvailable = kafkaAvailable.getAndSet(false);
            if (wasAvailable) {
                logger.warn("Kafka connection lost: {}. Switching to fallback if enabled.", e.getMessage());
            } else if (!kafkaOptional) {
                logger.error("Kafka is unavailable: {}. Set spring.kafka.bootstrap-servers-optional=true for fallback operation.", e.getMessage());
            }
        }
    }

    /**
     * Check if Kafka is currently available
     * @return true if Kafka is available, false otherwise
     */
    public boolean isKafkaAvailable() {
        return kafkaAvailable.get();
    }
}