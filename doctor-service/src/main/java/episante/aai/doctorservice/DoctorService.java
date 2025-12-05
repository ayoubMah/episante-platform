package episante.aai.doctorservice;

import com.upec.episantecommon.dto.DoctorProfileRequest;
import com.upec.episantecommon.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DoctorService {

    private final DoctorRepository repo;

    public DoctorService(DoctorRepository repo) {
        this.repo = repo;
    }

    public void createProfile(DoctorProfileRequest req) {
        Doctor d = new Doctor();
        d.setId(req.getId());
        d.setFirstName(req.getFirstName());
        d.setLastName(req.getLastName());
        d.setEmail(req.getEmail());
        d.setSpecialty(req.getSpecialty());
        d.setRpps(req.getRppsNumber());
        d.setClinicAddress(req.getAddress());
        d.setCreatedAt(OffsetDateTime.now());
        d.setUpdatedAt(OffsetDateTime.now());

        repo.save(d);
    }

    public List<Doctor> findAll() {
        return repo.findAll();
    }

    public Doctor findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Doctor not found"));
    }

    public Doctor create(Doctor d) {
        d.setCreatedAt(OffsetDateTime.now());
        d.setUpdatedAt(OffsetDateTime.now());
        return repo.save(d);
    }

    public Doctor update(UUID id, Doctor updated) {
        Doctor existing = findById(id);
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setSpecialty(updated.getSpecialty());
        existing.setRpps(updated.getRpps());
        existing.setClinicAddress(updated.getClinicAddress());
        existing.setUpdatedAt(OffsetDateTime.now());
        return repo.save(existing);
    }

    public void delete(UUID id) {
        repo.deleteById(id);
    }
}