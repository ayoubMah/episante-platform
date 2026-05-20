package com.upec.episantecommon.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

public record WearableMetricEvent(
        UUID patientId,
        int heartRate,
        int bloodPressureSystolic,
        int bloodPressureDiastolic,
        int spO2,
        double temperature,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp,

        String deviceId
) {}
