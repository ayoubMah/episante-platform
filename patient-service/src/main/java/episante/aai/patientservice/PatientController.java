package episante.aai.patientservice;

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
    public List<Patient> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public Patient one(@PathVariable UUID id) { return service.findById(id); }

    @PostMapping
    public Patient create(@RequestBody Patient p) { return service.create(p); }

    @PutMapping("/{id}")
    public Patient update(@PathVariable UUID id, @RequestBody Patient p) {
        return service.update(id, p);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
