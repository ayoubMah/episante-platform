package com.upec.ingestwearablesservice;

import com.upec.ingestwearablesservice.config.WearableTopicProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WearableTopicProperties.class)
public class IngestWearablesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestWearablesServiceApplication.class, args);
    }

}
