package com.upec.episantecommon.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable Event indicating an appointment was successfully created.
 */
public record AppointmentCreatedEvent(
        UUID appointmentId,
        UUID doctorId,
        UUID patientId,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime startTime,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime endTime,

        String status
) {}