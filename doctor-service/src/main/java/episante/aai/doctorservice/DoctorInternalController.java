package episante.aai.doctorservice;

import com.upec.episantecommon.dto.DoctorProfileRequest;
import com.upec.episantecommon.dto.DoctorResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/doctors")
public class DoctorInternalController {

    private final DoctorService service;

    public DoctorInternalController(DoctorService service) {
        this.service = service;
    }

    /**
     * Internal endpoint called by Auth-Service during registration.
     * MUST be blocked by API Gateway from external access.
     */
    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorResponseDTO createProfile(@RequestBody @Valid DoctorProfileRequest req) {
        // Reuse the logic we wrote in the Service layer
        // Note: Make sure your Service returns the Entity so we can map it back
        return toDTO(service.create(req));
    }

    // Simple mapper to keep it self-contained
    private DoctorResponseDTO toDTO(Doctor d) {
        DoctorResponseDTO dto = new DoctorResponseDTO();
        dto.setId(d.getId());
        dto.setFirstName(d.getFirstName());
        dto.setLastName(d.getLastName());
        dto.setEmail(d.getEmail());
        dto.setSpecialty(d.getSpecialty());
        return dto;
    }
}

