package com.upec.healthalertengine;

import com.upec.episantecommon.enums.AlertType;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertDedupCache {

    private static final Duration COOLDOWN = Duration.ofMinutes(5);

    private final Map<String, Instant> lastAlertTime = new ConcurrentHashMap<>();

    public boolean isDuplicate(UUID patientId, AlertType alertType) {
        String key = patientId + ":" + alertType.name();
        Instant last = lastAlertTime.get(key);
        if (last == null) return false;
        return Duration.between(last, Instant.now()).compareTo(COOLDOWN) < 0;
    }

    public void markSent(UUID patientId, AlertType alertType) {
        String key = patientId + ":" + alertType.name();
        lastAlertTime.put(key, Instant.now());
    }
}
