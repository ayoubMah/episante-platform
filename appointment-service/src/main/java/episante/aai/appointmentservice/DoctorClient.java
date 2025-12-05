package episante.aai.appointmentservice;

import com.upec.episantecommon.dto.DoctorResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "doctor-service",
        url = "${application.config.doctor-url}"
)
public interface DoctorClient {

    @GetMapping("/api/doctors/{id}")
    DoctorResponseDTO getDoctorById(@PathVariable UUID id);
}
