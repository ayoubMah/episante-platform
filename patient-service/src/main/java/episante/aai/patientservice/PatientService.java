package episante.aai.patientservice;

import com.upec.episantecommon.dto.PatientProfileRequest;
import com.upec.episantecommon.exception.DuplicateResourceException;
import com.upec.episantecommon.exception.ForbiddenException;
import com.upec.episantecommon.exception.NotFoundException;
import com.upec.episantecommon.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.upec.episantecommon.enums.Gender;

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

    public Page<Patient> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public Patient findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + id));
    }

    public List<Patient> findAllByIds(List<UUID> ids) {
        return repo.findAllById(ids);
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

        if (req.getGender() != null) {
            p.setGender(Gender.valueOf(req.getGender()));
        }

        p.setDoctorId(req.getDoctorId());

        return repo.save(p);
    }

    @Transactional
    public Patient update(UUID id, PatientProfileRequest req) {
        Patient existing = findById(id);

        // --- OWNERSHIP: only an admin can change the treating doctor or the login email (S-5) ---
        if (req.getEmail() == null || req.getEmail().equalsIgnoreCase(existing.getEmail())) {
            req.setEmail(existing.getEmail());
        }
        if (req.getDoctorId() == null) req.setDoctorId(existing.getDoctorId());

        if (!SecurityUtils.isAdmin()
                && (!existing.getEmail().equals(req.getEmail())
                    || !Objects.equals(existing.getDoctorId(), req.getDoctorId()))) {
            throw new ForbiddenException("Only an admin can change the email or doctor of a patient.");
        }

        existing.setFirstName(req.getFirstName());
        existing.setLastName(req.getLastName());
        existing.setEmail(req.getEmail());
        existing.setPhone(req.getPhone());

        if (req.getDob() != null) {
            existing.setDob(LocalDate.parse(req.getDob()));
        }

        if (req.getGender() != null) {
            existing.setGender(Gender.valueOf(req.getGender()));
        }

        existing.setDoctorId(req.getDoctorId());

        return repo.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new NotFoundException("Patient not found");
        repo.deleteById(id);
    }
}