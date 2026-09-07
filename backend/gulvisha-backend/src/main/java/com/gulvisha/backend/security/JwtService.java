package com.gulvisha.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiration;

    public JwtService(
            @Value("${app.jwt.secret:default-secret-key-change-me-in-production-min-32-chars}") String secret,
            @Value("${app.jwt.expiration:86400000}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(String username, List<String> permissions) {
        return generateToken(username, permissions, null, null, null);
    }

    public String generateToken(String username, List<String> permissions,
                                UUID organizationId, UUID clientId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        var builder = Jwts.builder()
                .subject(username)
                .claim("permissions", permissions)
                .claim("roles", permissions) // backward compatibility
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key);

        if (role != null) {
            builder.claim("role", role);
        }
        if (organizationId != null) {
            builder.claim("organizationId", organizationId.toString());
        }
        if (clientId != null) {
            builder.claim("clientId", clientId.toString());
        }

        return builder.compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
