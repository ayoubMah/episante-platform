package com.upec.episantecommon.dto;

import com.upec.episantecommon.enums.Gender;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class PatientResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;

    private String phone;
    private LocalDate dob;
    private Gender gender;
}
