package com.upec.episantecommon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isTokenValid(token)) {
            chain.doFilter(req, res);
            return;
        }

        String id = jwtService.extractUserId(token);
        String email = jwtService.extractUsername(token);
        String role = jwtService.extractRole(token);

        UserContext user = new UserContext(UUID.fromString(id), email, role);

        var auth = new UsernamePasswordAuthenticationToken(
                email, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        auth.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(req, res);
    }
}
