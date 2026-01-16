package episante.aai.doctorservice;

import com.upec.episantecommon.dto.DoctorProfileRequest;
import com.upec.episantecommon.dto.DoctorResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public List<DoctorResponseDTO> all() {
        return service.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public DoctorResponseDTO one(@PathVariable UUID id) {
        return toDTO(service.findById(id));
    }

    /**
     * Create Doctor Profile.
     * Usually called by Auth Service via Feign, or by Admin.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DoctorResponseDTO create(@RequestBody @Valid DoctorProfileRequest request) {
        return toDTO(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityRules.canEditDoctor(#id)")
    public DoctorResponseDTO update(@PathVariable UUID id, @RequestBody @Valid DoctorProfileRequest request) {
        // Ensure the ID in the path matches the intention (or ignore the ID in DTO)
        return toDTO(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Standard for delete
    @PreAuthorize("@securityRules.canDeleteDoctor(#id)")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    // Mapper
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