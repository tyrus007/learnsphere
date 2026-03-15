package com.ashique.learnsphere.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    public static final String CLAIM_USER_ID = "userId";

    public static final String CLAIM_EMAIL = "email";

    public static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UUID userId, String email, String role) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role)

                // --- Registered claims ---
                // subject is set to the userId string as a human-readable
                // identifier for the principal (standard JWT convention).
                .subject(userId.toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))

                // --- Signing ---
                // signWith(key) in 0.12.x auto-selects the strongest algorithm
                // supported by the provided key (HS256 for a 256-bit HMAC key).
                .signWith(signingKey)
                .compact();
    }

    public Claims extractClaims(String token) {
        // Jwts.parser() is the 0.12.x entry-point (replaces the removed
        // Jwts.parserBuilder() from 0.11.x).
        return Jwts.parser()
                .verifyWith(signingKey) // sets the key used for signature verification
                .build()
                .parseSignedClaims(token) // parses + validates in one call
                .getPayload(); // returns the Claims map
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException covers: ExpiredJwtException, MalformedJwtException,
            // SignatureException, UnsupportedJwtException.
            // IllegalArgumentException covers a null or empty token string.
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Typed claim accessors (convenience helpers for the filter and services)
    // -----------------------------------------------------------------------

    /**
     * Extracts the {@code userId} claim from an already-parsed {@link Claims}
     * object.
     *
     * @param claims the verified claims payload
     * @return the user's UUID
     */
    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
    }

    /**
     * Extracts the {@code email} claim from an already-parsed {@link Claims}
     * object.
     *
     * @param claims the verified claims payload
     * @return the user's email address
     */
    public String extractEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    /**
     * Extracts the {@code role} claim from an already-parsed {@link Claims} object.
     *
     * @param claims the verified claims payload
     * @return the user's role string (e.g. "STUDENT" or "INSTRUCTOR")
     */
    public String extractRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }
}