package com.upec.episantecommon.dto;

import com.upec.episantecommon.enums.AppointmentStatus;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AppointmentResponseDTO {
    private UUID id;
    private UUID doctorId;
    private UUID patientId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private AppointmentStatus status;
}
