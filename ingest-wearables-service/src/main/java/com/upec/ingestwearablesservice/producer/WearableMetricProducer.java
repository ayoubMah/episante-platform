package com.upec.ingestwearablesservice.producer;

import com.upec.episantecommon.event.WearableMetricEvent;
import com.upec.ingestwearablesservice.config.WearableTopicProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WearableMetricProducer {

    private static final Logger log = LoggerFactory.getLogger(WearableMetricProducer.class);

    private final KafkaTemplate<String, WearableMetricEvent> kafkaTemplate;
    private final WearableTopicProperties topicProps;

    public void send(WearableMetricEvent event) {
        String key = event.patientId().toString();

        kafkaTemplate.send(topicProps.topic(), key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish wearable metric for patient={} error={}",
                                event.patientId(), ex.getMessage(), ex);
                    } else {
                        log.debug("Metric published topic={} partition={} offset={} patientId={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                event.patientId());
                    }
                });
    }
}
