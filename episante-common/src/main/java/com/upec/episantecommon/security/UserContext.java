package com.upec.episantecommon.security;

import java.util.UUID;

public record UserContext(UUID id, String email, String role) {}
