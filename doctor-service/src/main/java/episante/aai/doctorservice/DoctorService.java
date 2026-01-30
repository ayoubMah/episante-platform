package episante.aai.doctorservice;

import com.upec.episantecommon.dto.DoctorProfileRequest;
import com.upec.episantecommon.exception.DuplicateResourceException;
import com.upec.episantecommon.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true) // Performance optimization
public class DoctorService {

    private final DoctorRepository repo;

    public DoctorService(DoctorRepository repo) {
        this.repo = repo;
    }

    // LIST / GET (Read Operations)
    public List<Doctor> findAll() {
        return repo.findAll();
    }

    public Doctor findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Doctor not found with ID: " + id));
    }

    /**
     * Creates a profile using the ID provided by the caller (Auth Service).
     */
    @Transactional // Write operation
    public Doctor create(DoctorProfileRequest req) {
        // 1. Identity Consistency Check
        if (req.getId() == null) {
            throw new IllegalArgumentException("Doctor ID must be provided (Shared Identity Pattern)");
        }

        // 2. Duplicate Check
        if (repo.existsById(req.getId())) {
            throw new DuplicateResourceException("Doctor profile already exists for ID: " + req.getId());
        }
        if (repo.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Email already taken: " + req.getEmail());
        }

        // 3. Mapping (DTO -> Entity)
        Doctor d = new Doctor();
        d.setId(req.getId()); // Explicitly setting the Shared ID
        d.setFirstName(req.getFirstName());
        d.setLastName(req.getLastName());
        d.setEmail(req.getEmail());
        d.setSpecialty(req.getSpecialty());
        d.setRpps(req.getRppsNumber());
        d.setClinicAddress(req.getAddress());

        // Timestamps handled by Database/Listener, but setting explicitly is safe too
        d.setCreatedAt(OffsetDateTime.now());
        d.setUpdatedAt(OffsetDateTime.now());

        return repo.save(d);
    }

    @Transactional
    public Doctor update(UUID id, DoctorProfileRequest req) {
        Doctor existing = findById(id);

        // Update fields
        existing.setFirstName(req.getFirstName());
        existing.setLastName(req.getLastName());
        existing.setSpecialty(req.getSpecialty());
        existing.setRpps(req.getRppsNumber());
        existing.setClinicAddress(req.getAddress());
        // Note: We typically do NOT update Email here without a verification process

        existing.setUpdatedAt(OffsetDateTime.now());

        return repo.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Doctor not found");
        }
        repo.deleteById(id);
    }
}