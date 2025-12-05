package episante.aai.patientservice;

import com.upec.episantecommon.security.BaseSecurityConfig;
import com.upec.episantecommon.security.JwtAuthenticationFilter;
import com.upec.episantecommon.security.JwtService;
import com.upec.episantecommon.security.SecurityRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;


@Configuration
@EnableWebSecurity
public class SecurityConfig extends BaseSecurityConfig {

    /**
     * These endpoints are NOT protected by JWT.
     * For patient-service: internal endpoints only.
     */
    @Override
    protected String[] publicEndpoints() {
        return new String[]{
                "/internal/**",   // internal microservice calls
                "/actuator/health"
        };
    }

    /**
     * JwtService bean coming from episante-common
     */
    @Bean
    public JwtService jwtService(@Value("${application.security.jwt-secret}") String secret) {
        return new JwtService(secret);
    }

    /**
     * JwtAuthenticationFilter bean coming from episante-common
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    /**
     * Inject SecurityRules so @securityRules.xxx works in @PreAuthorize
     */
    @Bean
    public SecurityRules securityRules() {
        return new SecurityRules();
    }
}