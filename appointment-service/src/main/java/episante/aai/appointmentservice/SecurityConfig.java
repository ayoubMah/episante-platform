package episante.aai.appointmentservice;

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
    @Override
    protected String[] publicEndpoints() {
        return new String[]{
                "/actuator/**",
                "/internal/**"
        };
    }

    @Bean
    public JwtService jwtService(@Value("${application.security.jwt-secret}") String secret) {
        return new JwtService(secret);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public SecurityRules securityRules() {
        return new SecurityRules();
    }
}