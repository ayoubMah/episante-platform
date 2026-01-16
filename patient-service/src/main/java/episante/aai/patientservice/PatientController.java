package episante.aai.patientservice;

import com.upec.episantecommon.dto.PatientResponseDTO;
import com.upec.episantecommon.dto.PatientProfileRequest;
import com.upec.episantecommon.security.SecurityRules;
import jakarta.validation.Valid;
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
    public List<PatientResponseDTO> all() {
        return service.findAll().stream().map(this::toDTO).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityRules.canViewPatient(#id)")
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

        return dto;
    }
}