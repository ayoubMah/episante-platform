package com.upec.ingestwearablesservice.controller;

import com.upec.episantecommon.event.WearableMetricEvent;
import com.upec.ingestwearablesservice.producer.WearableMetricProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wearable")
@RequiredArgsConstructor
public class WearableController {

    private static final Logger log = LoggerFactory.getLogger(WearableController.class);

    private final WearableMetricProducer producer;

    @PostMapping("/metrics")
    @ResponseStatus(HttpStatus.CREATED)
    public void ingestMetric(@RequestBody WearableMetricEvent metric) {
        log.info("Received metric patientId={} heartRate={} bp={}/{} spO2={} temp={}",
                metric.patientId(), metric.heartRate(),
                metric.bloodPressureSystolic(), metric.bloodPressureDiastolic(),
                metric.spO2(), metric.temperature());

        producer.send(metric);
    }
}
