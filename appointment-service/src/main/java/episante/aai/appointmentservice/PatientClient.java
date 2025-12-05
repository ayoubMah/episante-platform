package episante.aai.appointmentservice;

import com.upec.episantecommon.dto.PatientResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


import java.util.UUID;

@FeignClient(
        name = "patient-service",
        url = "${application.config.patient-url}"
)
public interface PatientClient {

    // Fetch patient by ID to verify existence
    @GetMapping("/api/patients/{id}")
    PatientResponseDTO getPatientById(@PathVariable UUID id);
}