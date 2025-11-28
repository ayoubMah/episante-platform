package episante.aai.patientservice;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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

    /**
     * Get all patients
     * - Admin
     * - Doctor
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public List<Patient> all() {
        return service.findAll();
    }

    /**
     * Get one patient
     * - Self
     * - Doctor
     * - Admin
     */
    @GetMapping("/{id}")
    @PreAuthorize("@securityRules.canViewPatient(#id)")
    public Patient one(@PathVariable UUID id) {
        return service.findById(id);
    }

    /**
     * Create patient (internal only)
     */
    @PostMapping
    @PreAuthorize("permitAll()") // internal call only
    public Patient create(@RequestBody Patient p) {
        return service.create(p);
    }

    /**
     * Update patient
     * - Self
     * - Admin
     */
    @PutMapping("/{id}")
    @PreAuthorize("@securityRules.isSelf(#id) or hasRole('ADMIN')")
    public Patient update(@PathVariable UUID id, @RequestBody Patient p) {
        return service.update(id, p);
    }

    /**
     * Delete patient
     * - Admin only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
