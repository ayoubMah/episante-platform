package episante.aai.appointmentservice;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;

    /**
     * Create a new appointment (status = PLANNED)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponseDTO create(@RequestBody CreateAppointmentRequestDTO request) {
        return service.create(request);
    }

    /**
     * Get a raw appointment (doctorId + patientId only)
     */
    @GetMapping("/{id}")
    public AppointmentResponseDTO getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    /**
     * Get a fully enriched appointment detail (doctor name, patient name, specialty)
     * This is the aggregator method used by the frontend UI.
     */
    @GetMapping("/{id}/details")
    public AppointmentDetailsDTO getDetails(@PathVariable UUID id) {
        return service.getAppointmentDetails(id);
    }

    /**
     * Optional: Get all appointments (for calendar/listing views)
     * The frontend usually needs a list view first.
     */
    @GetMapping
    public List<AppointmentResponseDTO> getAll() {
        return service.getAll();
    }
}