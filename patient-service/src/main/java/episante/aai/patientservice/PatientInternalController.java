package episante.aai.patientservice;

import com.upec.episantecommon.dto.PatientProfileRequest;
import com.upec.episantecommon.dto.PatientResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/patients")
public class PatientInternalController {

    private final PatientService service;

    public PatientInternalController(PatientService service) {
        this.service = service;
    }

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponseDTO createProfile(@RequestBody @Valid PatientProfileRequest req) {
        return toDTO(service.createProfile(req));
    }

    @PostMapping("/batch")
    public List<PatientResponseDTO> getPatientsByIds(@RequestBody List<UUID> ids) {
        return service.findAllByIds(ids).stream().map(this::toDTO).toList();
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