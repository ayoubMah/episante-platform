package com.upec.healthalertengine;

import com.upec.episantecommon.enums.Gender;

import java.time.LocalDate;
import java.util.UUID;

public record PatientProfile(
        UUID patientId,
        LocalDate dob,
        Gender gender,
        UUID doctorId
) {}
