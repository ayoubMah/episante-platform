package com.upec.episantecommon.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

public class JwtService {

    private String jwtSecret;

    public JwtService(String secret) {
        this.jwtSecret = secret;
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("id", String.class));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public boolean isTokenValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String generateToken(UUID userId, String role) {
        return Jwts.builder()
                .setSubject(userId.toString())          // standard claim: “sub”
                .claim("id", userId.toString())         // custom claim
                .claim("role", role)                    // custom claim
                .setIssuedAt(new Date())                 // now
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24h
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parse(token));
    }
}
