package episante.aai.authservice;

import com.upec.episantecommon.enums.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Import this!

import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional // Ensure DB transaction
    public void run(String... args) throws Exception {
        // 1. Check if admin exists
        if (!userRepository.existsByEmail("admin@admin.com")) {

            // 2. Create Admin
            User admin = new User();
            admin.setId(UUID.randomUUID()); // Explicit ID
            admin.setEmail("admin@admin.com");
            admin.setPassword(passwordEncoder.encode("admin")); // Static password
            admin.setRole(Role.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);

            System.out.println("👑 SUPER ADMIN CREATED: admin@admin.com / admin");
        }
    }
}