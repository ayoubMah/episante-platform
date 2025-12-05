package episante.aai.appointmentservice;

import com.upec.episantecommon.dto.AppointmentDetailsDTO;
import com.upec.episantecommon.dto.AppointmentResponseDTO;
import com.upec.episantecommon.dto.CreateAppointmentRequestDTO;
import com.upec.episantecommon.security.SecurityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;
    private final SecurityRules rules;

    /**
     * Create a new appointment
     * Only PATIENT or ADMIN can create.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityRules.canCreate()")
    public AppointmentResponseDTO create(@RequestBody CreateAppointmentRequestDTO request) {
        return service.create(request);
    }

    /**
     * Get appointment by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("@securityRules.canView(@appointmentService.getPatientId(#id), @appointmentService.getDoctorId(#id))")
    public AppointmentResponseDTO getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    /**
     * Get appointment details (doctor + patient)
     */
    @GetMapping("/{id}/details")
    @PreAuthorize("@securityRules.canView(@appointmentService.getPatientId(#id), @appointmentService.getDoctorId(#id))")
    public AppointmentDetailsDTO getDetails(@PathVariable UUID id) {
        return service.getAppointmentDetails(id);
    }

    /**
     * Smart filtering by ROLE
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('PATIENT')")
    public List<AppointmentResponseDTO> getAll() {
        return service.getAllForCurrentUser();
    }

    /**
     * Update appointment
     */
    @PutMapping("/{id}")
    @PreAuthorize("@securityRules.canEdit(@appointmentService.getPatientId(#id), @appointmentService.getDoctorId(#id))")
    public AppointmentResponseDTO update(@PathVariable UUID id,
                                         @RequestBody CreateAppointmentRequestDTO request) {
        return service.update(id, request);
    }

    /**
     * Delete appointment
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@securityRules.canDelete(@appointmentService.getPatientId(#id), @appointmentService.getDoctorId(#id))")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
