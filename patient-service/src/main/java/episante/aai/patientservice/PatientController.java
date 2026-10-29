package episante.aai.patientservice;

import com.upec.episantecommon.dto.PageResponse;
import com.upec.episantecommon.dto.PatientResponseDTO;
import com.upec.episantecommon.dto.PatientProfileRequest;
import com.upec.episantecommon.security.SecurityRules;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public PageResponse<PatientResponseDTO> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return PageResponse.of(
                service.findAllForCurrentUser(PageRequest.of(page, size)).map(this::toDTO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityRules.isSelf(#id) or hasRole('ADMIN') or @patientService.isPatientOfCurrentDoctor(#id)")
    public PatientResponseDTO one(@PathVariable UUID id) {
        return toDTO(service.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityRules.isSelf(#id) or hasRole('ADMIN')")
    public PatientResponseDTO update(@PathVariable UUID id, @RequestBody @Valid PatientProfileRequest req) {
        return toDTO(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    private PatientResponseDTO toDTO(Patient p) {
        PatientResponseDTO dto = new PatientResponseDTO();
        dto.setId(p.getId());
        dto.setFirstName(p.getFirstName());
        dto.setLastName(p.getLastName());
        dto.setEmail(p.getEmail());
        dto.setPhone(p.getPhone());
        dto.setDob(p.getDob());
        dto.setGender(p.getGender());
        dto.setDoctorId(p.getDoctorId());

        return dto;
    }
}