package episante.aai.authservice;

import com.upec.episantecommon.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// S-1: defense in depth — the service itself refuses ADMIN, whatever the caller
@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PatientClient patientClient;
    @Mock private DoctorClient doctorClient;

    @InjectMocks private AuthService authService;

    @Test
    void registerAdmin_throwsBadRequest_andPersistsNothing() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("evil@example.com");
        request.setPassword("secret123");
        request.setRole(Role.ADMIN);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(patientClient, doctorClient, passwordEncoder);
    }
}
