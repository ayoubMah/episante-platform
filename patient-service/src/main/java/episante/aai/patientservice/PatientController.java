package episante.aai.patientservice;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.upec.episantecommon.security.SecurityRules;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService service;
    private final SecurityRules rules;

    public PatientController(PatientService service, SecurityRules rules) {
        this.service = service;
        this.rules = rules;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public List<Patient> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityRules.canViewPatient(#id)")
    public Patient one(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityRules.isSelf(#id) or hasRole('ADMIN')")
    public Patient update(@PathVariable UUID id, @RequestBody Patient p) {
        return service.update(id, p);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}