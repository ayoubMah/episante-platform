package episante.aai.doctorservice;

import com.upec.episantecommon.dto.DoctorResponseDTO;
import com.upec.episantecommon.security.SecurityRules;
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
    public List<DoctorResponseDTO> all() {
        return service.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Public: get doctor details by ID
     */
    @GetMapping("/{id}")
    public DoctorResponseDTO one(@PathVariable UUID id) {
        return toDTO(service.findById(id));
    }

    /**
     * Create doctor (internal from auth-service or admin)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public DoctorResponseDTO create(@RequestBody Doctor d) {
        return toDTO(service.create(d));
    }

    /**
     * Update doctor
     * - Admin
     * - Doctor editing own profile
     */
    @PutMapping("/{id}")
    @PreAuthorize("@securityRules.canEditDoctor(#id)")
    public DoctorResponseDTO update(@PathVariable UUID id, @RequestBody Doctor d) {
        return toDTO(service.update(id, d));
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

    // DTO mapping
    private DoctorResponseDTO toDTO(Doctor d) {
        DoctorResponseDTO dto = new DoctorResponseDTO();
        dto.setId(d.getId());
        dto.setFirstName(d.getFirstName());
        dto.setLastName(d.getLastName());
        dto.setSpecialty(d.getSpecialty());
        dto.setEmail(d.getEmail());
        return dto;
    }
}