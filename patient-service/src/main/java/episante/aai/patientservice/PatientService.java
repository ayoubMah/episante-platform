package episante.aai.patientservice;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {
    private final PatientRepository repo;

    public PatientService(PatientRepository repo) {
        this.repo = repo;
    }

    public List<Patient> findAll() { return repo.findAll(); }

    public Patient findById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    public Patient create(Patient p) { return repo.save(p); }

    public Patient update(UUID id, Patient updated) {
        Patient existing = findById(id);
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setDob(updated.getDob());
        existing.setGender(updated.getGender());
        existing.setUpdatedAt(updated.getUpdatedAt());
        return repo.save(existing);
    }

    public void delete(UUID id) { repo.deleteById(id); }
}
