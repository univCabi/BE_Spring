package org.univcabi.univcabi.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Define topic names as constants
    public static final String RENT_TOPIC = "cabinet-rent-requests";
    public static final String RETURN_TOPIC = "cabinet-return-requests";
    public static final String STATUS_UPDATE_TOPIC = "cabinet-status-updates";

    // Create topics with appropriate configurations
    @Bean
    public NewTopic rentRequestsTopic() {
        return TopicBuilder.name(RENT_TOPIC)
                .partitions(3)   // Multiple partitions for scalability
                .replicas(1)     // Increase in production for redundancy
                .compact()       // Compact the topic to save space
                .build();
    }

    @Bean
    public NewTopic returnRequestsTopic() {
        return TopicBuilder.name(RETURN_TOPIC)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }

    @Bean
    public NewTopic statusUpdatesTopic() {
        return TopicBuilder.name(STATUS_UPDATE_TOPIC)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
}