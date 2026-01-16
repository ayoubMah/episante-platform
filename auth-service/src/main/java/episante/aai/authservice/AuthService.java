package episante.aai.authservice;

import com.upec.episantecommon.dto.DoctorProfileRequest;
import com.upec.episantecommon.dto.PatientProfileRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientClient patientClient;
    private final DoctorClient doctorClient;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       PatientClient patientClient,
                       DoctorClient doctorClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.patientClient = patientClient;
        this.doctorClient = doctorClient;
    }

    public UUID register(RegisterRequest request) {

        // 1. Check if email exists (Fail fast)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        UUID userId = UUID.randomUUID();

        // 2. Create the User Entity
        User user = new User();
        user.setId(userId);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setActive(true);

        // 3. Save User FIRST (The "Pending" State)
        // We save to DB immediately. If this fails, we stop before calling other services.
        userRepository.save(user);

        try {
            // 4. Call Downstream Service (The "Orchestration")
            switch (request.getRole()) {
                case PATIENT -> {
                    PatientProfileRequest dto = new PatientProfileRequest();
                    dto.setId(userId); // Shared ID
                    dto.setFirstName(request.getFirstName());
                    dto.setLastName(request.getLastName());
                    dto.setEmail(request.getEmail());
                    dto.setDob(request.getDob());
                    dto.setPhone(request.getPhone());

                    patientClient.createPatientProfile(dto);
                }
                case DOCTOR -> {
                    DoctorProfileRequest dto = new DoctorProfileRequest();
                    dto.setId(userId); // Shared ID
                    dto.setFirstName(request.getFirstName());
                    dto.setLastName(request.getLastName());
                    dto.setEmail(request.getEmail());
                    dto.setSpecialty(request.getSpecialty());
                    dto.setRppsNumber(request.getRppsNumber());
                    dto.setAddress(request.getAddress());

                    doctorClient.createDoctorProfile(dto);
                }
            }
        } catch (Exception e) {
            // 5. COMPENSATING TRANSACTION
            // The remote call failed, so we must delete the local User to maintain consistency.
            userRepository.deleteById(userId);
            throw new RuntimeException("Registration failed upstream: " + e.getMessage());
        }

        return userId;
    }
}

