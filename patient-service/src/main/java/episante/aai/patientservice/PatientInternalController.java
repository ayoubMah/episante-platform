package episante.aai.patientservice;

import com.upec.episantecommon.dto.PatientProfileRequest;
import com.upec.episantecommon.dto.PatientResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
        Patient created = service.createProfile(req);

        // Manual mapping again (In real projects, use MapStruct)
        PatientResponseDTO dto = new PatientResponseDTO();
        dto.setId(created.getId());
        dto.setFirstName(created.getFirstName());
        dto.setLastName(created.getLastName());
        dto.setEmail(created.getEmail());
        return dto;
    }
}