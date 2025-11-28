package episante.aai.authservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "doctor-service",
        url = "${application.config.doctor-url}"
)
public interface DoctorClient {

    @PostMapping("/internal/doctors/profile")
    void createDoctorProfile(DoctorProfileRequest request);
}

