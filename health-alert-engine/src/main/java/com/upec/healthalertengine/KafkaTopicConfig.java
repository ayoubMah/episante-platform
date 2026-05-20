package com.upec.healthalertengine;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic patientAlertTopic() {
        return TopicBuilder.name("health.alerts.patient")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic doctorAlertTopic() {
        return TopicBuilder.name("health.alerts.doctor")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
