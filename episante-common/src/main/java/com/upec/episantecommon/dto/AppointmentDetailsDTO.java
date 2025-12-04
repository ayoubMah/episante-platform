package com.upec.episantecommon.dto;

import com.upec.episantecommon.enums.AppointmentStatus;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AppointmentDetailsDTO {
    private UUID id;

    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private AppointmentStatus status;

    // Doctor info
    private UUID doctorId;
    private String doctorFullName;
    private String doctorSpecialty;

    // Patient info
    private UUID patientId;
    private String patientFullName;
}

