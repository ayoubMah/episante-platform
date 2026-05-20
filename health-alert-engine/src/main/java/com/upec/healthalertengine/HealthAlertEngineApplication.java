package com.upec.healthalertengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class HealthAlertEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthAlertEngineApplication.class, args);
    }

}
