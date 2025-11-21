package episante.aai.appointmentservice;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;

    @PostMapping
    public AppointmentResponseDTO create(@RequestBody CreateAppointmentRequestDTO request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public AppointmentResponseDTO getById(@PathVariable UUID id) {
        return service.getById(id);
    }
}
