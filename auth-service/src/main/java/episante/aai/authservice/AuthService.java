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

    @Transactional
    public UUID register(RegisterRequest request) {

        UUID userId = UUID.randomUUID();

        // --- Save user in Auth DB ---
        User user = new User();
        user.setId(userId);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        userRepository.save(user);

        try {
            switch (request.getRole()) {

                case PATIENT -> {
                    // Build DTO manually (NO ARGS constructor required)
                    PatientProfileRequest dto = new PatientProfileRequest();
                    dto.setId(userId);
                    dto.setFirstName(request.getFirstName());
                    dto.setLastName(request.getLastName());
                    dto.setEmail(request.getEmail());
                    dto.setDob(request.getDob());
                    dto.setPhone(request.getPhone());

                    patientClient.createPatientProfile(dto);
                }

                case DOCTOR -> {
                    DoctorProfileRequest dto = new DoctorProfileRequest();
                    dto.setId(userId);
                    dto.setFirstName(request.getFirstName());
                    dto.setLastName(request.getLastName());
                    dto.setEmail(request.getEmail());
                    dto.setSpecialty(request.getSpecialty());
                    dto.setRppsNumber(request.getRppsNumber());
                    dto.setAddress(request.getAddress());

                    doctorClient.createDoctorProfile(dto);
                }

                case ADMIN -> {
                    // No profile creation required
                }
            }

        } catch (Exception ex) {

            // SAGA rollback
            userRepository.deleteById(userId);

            throw new RuntimeException("Registration failed: " + ex.getMessage());
        }

        return userId;
    }
}

