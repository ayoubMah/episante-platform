package com.upec.episantecommon.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.upec.episantecommon.enums.AlertSeverity;
import com.upec.episantecommon.enums.AlertType;

import java.time.Instant;
import java.util.UUID;

public record HealthAlertEvent(
        UUID alertId,
        UUID patientId,
        UUID doctorId,
        AlertType alertType,
        AlertSeverity severity,
        String message,
        double actualValue,
        double thresholdUsed,
        String metricName,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp
) {}
