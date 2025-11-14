package episante.aai.patientservice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
}


