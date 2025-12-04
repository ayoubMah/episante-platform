package com.upec.episantecommon.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PatientResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
}
