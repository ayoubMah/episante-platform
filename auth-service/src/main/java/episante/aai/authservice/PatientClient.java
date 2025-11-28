package episante.aai.authservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "patient-service",
        url = "${application.config.patient-url}"
)
public interface PatientClient {

    @PostMapping("/internal/patients/profile")
    void createPatientProfile(PatientProfileRequest request);
}


