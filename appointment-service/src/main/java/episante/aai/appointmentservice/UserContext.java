package episante.aai.appointmentservice;

import java.util.UUID;

public record UserContext(UUID id, String email, String role) {}
