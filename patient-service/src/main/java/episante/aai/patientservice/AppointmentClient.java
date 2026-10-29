package episante.aai.patientservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "appointment-service",
        url = "${application.config.appointment-url}"
)
public interface AppointmentClient {

    // IDs of the patients the current doctor (from the forwarded JWT) has appointments with
    @GetMapping("/api/appointments/my-patients")
    List<UUID> getMyPatientIds();
}
