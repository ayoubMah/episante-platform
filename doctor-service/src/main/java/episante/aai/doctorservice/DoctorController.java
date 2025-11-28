package episante.aai.doctorservice;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService service;
    private final SecurityRules rules;

    public DoctorController(DoctorService service, SecurityRules rules) {
        this.service = service;
        this.rules = rules;
    }

    /**
     * Public: list all doctors
     */
    @GetMapping
    public List<Doctor> all() {
        return service.findAll();
    }

    /**
     * Public: get doctor details by ID
     */
    @GetMapping("/{id}")
    public Doctor one(@PathVariable UUID id) {
        return service.findById(id);
    }

    /**
     * Create doctor (internal from auth-service or admin)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public Doctor create(@RequestBody Doctor d) {
        return service.create(d);
    }

    /**
     * Update doctor
     * - Admin
     * - Doctor editing own profile
     */
    @PutMapping("/{id}")
    @PreAuthorize("@securityRules.canEditDoctor(#id)")
    public Doctor update(@PathVariable UUID id, @RequestBody Doctor d) {
        return service.update(id, d);
    }

    /**
     * Delete doctor
     * - Admin only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@securityRules.canDeleteDoctor(#id)")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
