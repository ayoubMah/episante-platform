package episante.aai.patientservice;

import com.upec.episantecommon.dto.PatientProfileRequest;
import com.upec.episantecommon.exception.DuplicateResourceException;
import com.upec.episantecommon.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository repo;

    public PatientService(PatientRepository repo) {
        this.repo = repo;
    }

    public List<Patient> findAll() {
        return repo.findAll();
    }

    public Patient findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + id));
    }

    @Transactional
    public Patient createProfile(PatientProfileRequest req) {
        if (repo.existsById(req.getId())) {
            throw new DuplicateResourceException("Profile already exists");
        }

        Patient p = new Patient();
        p.setId(req.getId());
        p.setFirstName(req.getFirstName());
        p.setLastName(req.getLastName());
        p.setEmail(req.getEmail());
        p.setPhone(req.getPhone());

        // Safe Date Parsing
        if (req.getDob() != null) {
            try {
                p.setDob(LocalDate.parse(req.getDob()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD");
            }
        }

        return repo.save(p);
    }

    @Transactional
    public Patient update(UUID id, PatientProfileRequest req) {
        Patient existing = findById(id);

        // We generally use DTOs for updates too.
        // For now, reusing ProfileRequest is okay, but usually, we have UpdatePatientRequest.
        existing.setFirstName(req.getFirstName());
        existing.setLastName(req.getLastName());
        existing.setPhone(req.getPhone());

        if (req.getDob() != null) {
            existing.setDob(LocalDate.parse(req.getDob()));
        }

        return repo.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new NotFoundException("Patient not found");
        repo.deleteById(id);
    }
}