package com.upec.healthalertengine;

import com.upec.episantecommon.enums.AlertSeverity;
import com.upec.episantecommon.enums.AlertType;
import com.upec.episantecommon.event.HealthAlertEvent;
import com.upec.episantecommon.event.WearableMetricEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
public class AlertStreamTopology {

    private static final Logger log = LoggerFactory.getLogger(AlertStreamTopology.class);

    private final ThresholdEngine thresholdEngine;
    private final PatientProfileFetcher profileFetcher;
    private final AlertController alertController;
    private final AlertDedupCache dedupCache;

    public AlertStreamTopology(ThresholdEngine thresholdEngine,
                               PatientProfileFetcher profileFetcher,
                               AlertController alertController) {
        this.thresholdEngine = thresholdEngine;
        this.profileFetcher = profileFetcher;
        this.alertController = alertController;
        this.dedupCache = new AlertDedupCache();
    }

    @Bean
    public KStream<String, WearableMetricEvent> alertTopology(StreamsBuilder builder) {
        var keySerde = Serdes.String();

        var serdeProps = new HashMap<String, Object>();
        serdeProps.put("spring.json.use.type.headers", false);
        var metricSerde = new JsonSerde<>(WearableMetricEvent.class);
        metricSerde.configure(serdeProps, false);
        var alertSerde = new JsonSerde<>(HealthAlertEvent.class);
        alertSerde.configure(serdeProps, false);

        KStream<String, WearableMetricEvent> input = builder.stream(
                "wearable.telemetry.raw",
                Consumed.with(keySerde, metricSerde)
        );

        KStream<String, HealthAlertEvent> alerts = input
                .flatMapValues((key, metric) -> evaluate(metric))
                .filter((key, alert) -> alert != null);

        alerts.filter((key, alert) -> true)
                .to("health.alerts.patient", Produced.with(keySerde, alertSerde));

        alerts.filter((key, alert) -> alert.doctorId() != null)
                .to("health.alerts.doctor", Produced.with(keySerde, alertSerde));

        return input;
    }

    private List<HealthAlertEvent> evaluate(WearableMetricEvent metric) {
        PatientProfile profile = profileFetcher.getProfile(metric.patientId());
        if (profile == null) {
            log.warn("No profile found for patient {}", metric.patientId());
            return List.of();
        }

        ThresholdEngine.Thresholds thresholds = thresholdEngine.getThresholds(profile.dob(), profile.gender());
        List<HealthAlertEvent> alerts = new ArrayList<>();

        // Heart Rate
        if (metric.heartRate() < thresholds.hrMin()) {
            alerts.add(buildAlert(metric, profile, AlertType.LOW_HEART_RATE,
                    AlertSeverity.WARNING, metric.heartRate(), thresholds.hrMin(), "heartRate"));
        } else if (metric.heartRate() > thresholds.hrMax()) {
            AlertSeverity sev = metric.heartRate() > thresholds.hrMax() + 20
                    ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
            alerts.add(buildAlert(metric, profile, AlertType.HIGH_HEART_RATE,
                    sev, metric.heartRate(), thresholds.hrMax(), "heartRate"));
        }

        // BP Systolic
        if (metric.bloodPressureSystolic() > thresholds.bpSystolicMax()) {
            AlertSeverity sev = metric.bloodPressureSystolic() > thresholds.bpSystolicMax() + 20
                    ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
            alerts.add(buildAlert(metric, profile, AlertType.HIGH_BP_SYSTOLIC,
                    sev, metric.bloodPressureSystolic(), thresholds.bpSystolicMax(), "bloodPressureSystolic"));
        }

        // BP Diastolic
        if (metric.bloodPressureDiastolic() > thresholds.bpDiastolicMax()) {
            AlertSeverity sev = metric.bloodPressureDiastolic() > thresholds.bpDiastolicMax() + 15
                    ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
            alerts.add(buildAlert(metric, profile, AlertType.HIGH_BP_DIASTOLIC,
                    sev, metric.bloodPressureDiastolic(), thresholds.bpDiastolicMax(), "bloodPressureDiastolic"));
        }

        // SpO2
        if (metric.spO2() < thresholds.spO2Min()) {
            AlertSeverity sev = metric.spO2() < 90 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
            alerts.add(buildAlert(metric, profile, AlertType.LOW_OXYGEN_LEVEL,
                    sev, metric.spO2(), thresholds.spO2Min(), "spO2"));
        }

        // Temperature high
        if (metric.temperature() > thresholds.tempMax()) {
            AlertSeverity sev = metric.temperature() > 39.5 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
            alerts.add(buildAlert(metric, profile, AlertType.HIGH_TEMPERATURE,
                    sev, metric.temperature(), thresholds.tempMax(), "temperature"));
        }

        // Temperature low
        if (metric.temperature() < thresholds.tempMin()) {
            AlertSeverity sev = metric.temperature() < 35.0 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
            alerts.add(buildAlert(metric, profile, AlertType.LOW_TEMPERATURE,
                    sev, metric.temperature(), thresholds.tempMin(), "temperature"));
        }

        List<HealthAlertEvent> deduped = new ArrayList<>();
        for (HealthAlertEvent alert : alerts) {
            if (!dedupCache.isDuplicate(metric.patientId(), alert.alertType())) {
                dedupCache.markSent(metric.patientId(), alert.alertType());
                deduped.add(alert);
                alertController.bufferAlert(alert);
            }
        }

        return deduped;
    }

    private HealthAlertEvent buildAlert(WearableMetricEvent metric, PatientProfile profile,
                                        AlertType type, AlertSeverity severity,
                                        double actualValue, double thresholdUsed, String metricName) {
        return new HealthAlertEvent(
                UUID.randomUUID(),
                metric.patientId(),
                profile.doctorId(),
                type,
                severity,
                String.format("%s: %.1f (threshold: %.1f)", metricName, actualValue, thresholdUsed),
                actualValue,
                thresholdUsed,
                metricName,
                Instant.now()
        );
    }
}
