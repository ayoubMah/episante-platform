package episante.aai.doctorservice;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService service;

    public DoctorController(DoctorService service) {
        this.service = service;
    }

    @GetMapping
    public List<Doctor> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Doctor one(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public Doctor create(@RequestBody Doctor d) {
        return service.create(d);
    }

    @PutMapping("/{id}")
    public Doctor update(@PathVariable UUID id, @RequestBody Doctor d) {
        return service.update(id, d);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
