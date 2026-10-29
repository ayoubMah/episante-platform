package episante.aai.patientservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Page<Patient> findByDoctorId(UUID doctorId, Pageable pageable);

    Page<Patient> findByDoctorIdOrIdIn(UUID doctorId, Collection<UUID> ids, Pageable pageable);
}
