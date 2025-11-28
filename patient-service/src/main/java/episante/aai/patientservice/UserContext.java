package episante.aai.patientservice;

import java.util.UUID;

public record UserContext(UUID id, String email, String role) {}
