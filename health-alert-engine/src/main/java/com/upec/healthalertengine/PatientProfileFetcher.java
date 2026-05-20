package com.upec.healthalertengine;

import com.upec.episantecommon.dto.PatientResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PatientProfileFetcher {

    private static final Logger log = LoggerFactory.getLogger(PatientProfileFetcher.class);

    private final RestTemplate restTemplate;
    private final String patientServiceUrl;
    private final Map<UUID, PatientProfile> cache = new ConcurrentHashMap<>();

    public PatientProfileFetcher(RestTemplate restTemplate,
                                 @Value("${patient.service.url}") String patientServiceUrl) {
        this.restTemplate = restTemplate;
        this.patientServiceUrl = patientServiceUrl;
    }

    public PatientProfile getProfile(UUID patientId) {
        return cache.computeIfAbsent(patientId, this::fetchProfile);
    }

    private PatientProfile fetchProfile(UUID patientId) {
        try {
            PatientResponseDTO[] response = restTemplate.postForObject(
                    patientServiceUrl + "/internal/patients/batch",
                    Collections.singletonList(patientId),
                    PatientResponseDTO[].class);

            if (response != null && response.length > 0) {
                PatientResponseDTO dto = response[0];
                PatientProfile profile = new PatientProfile(
                        dto.getId(),
                        dto.getDob(),
                        dto.getGender(),
                        dto.getDoctorId()
                );
                log.info("Fetched patient profile: {}", profile.patientId());
                return profile;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch patient profile for {}: {}", patientId, e.getMessage());
        }
        return null;
    }
}
