package com.ashique.learnsphere.jwt;

import java.util.UUID;

/**
 * The authenticated principal extracted from a validated JWT.
 * Set in {@link SecurityContextHolder} by {@link JwtAuthenticationFilter}.
 *
 * <p>Retrieve in controllers/services via:
 * <pre>{@code
 * AuthenticatedUser user = (AuthenticatedUser)
 *     SecurityContextHolder.getContext().getAuthentication().getPrincipal();
 * }</pre>
 */
public record AuthenticatedUser(
        UUID userId,
        String email,
        String role
) {}
