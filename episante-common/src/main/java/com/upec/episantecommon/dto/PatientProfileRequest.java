package com.upec.episantecommon.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PatientProfileRequest {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String dob;
    private String phone;
}

