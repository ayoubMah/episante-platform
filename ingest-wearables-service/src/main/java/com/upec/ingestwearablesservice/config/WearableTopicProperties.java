package com.upec.ingestwearablesservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ingest.wearable")
public record WearableTopicProperties(String topic) {}
