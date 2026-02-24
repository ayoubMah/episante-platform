package com.upec.episantecommon.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable Event indicating an appointment was successfully created.
 */
public record AppointmentCreatedEvent(
        UUID appointmentId,
        UUID doctorId,
        UUID patientId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String status
) {}