package episante.aai.authservice;

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

        // 1. Generate ID for both auth + profile services
        UUID userId = UUID.randomUUID();

        // 2. Save in Auth DB
        User user = new User();
        user.setId(userId);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        userRepository.save(user); // ✔ persisted

        try {
            // 3. Create the profile in the correct service
            switch (request.getRole()) {

                case PATIENT -> {
                    patientClient.createPatientProfile(
                            new PatientProfileRequest(
                                    userId,
                                    request.getFirstName(),
                                    request.getLastName(),
                                    request.getEmail(),
                                    request.getDob(),
                                    request.getPhone()
                            )
                    );
                }

                case DOCTOR -> {
                    doctorClient.createDoctorProfile(
                            new DoctorProfileRequest(
                                    userId,
                                    request.getFirstName(),
                                    request.getLastName(),
                                    request.getEmail(),
                                    request.getSpecialty(),
                                    request.getRppsNumber(),
                                    request.getAddress()
                            )
                    );
                }

                case ADMIN -> {
                    // No profile needed → just return
                }
            }

        } catch (Exception ex) {

            // 4. COMPENSATING ACTION (SAGA)
            userRepository.deleteById(userId);

            throw new RuntimeException("Registration failed: " + ex.getMessage());
        }

        return userId;
    }
}

