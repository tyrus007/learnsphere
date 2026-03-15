package com.ashique.learnsphere.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Servlet filter that authenticates every incoming HTTP request using the
 * JWT found in the {@code Authorization: Bearer <token>} header.
 *
 * <p>Registered as a Spring-managed {@code @Component} so that each service's
 * {@code SecurityConfig} can inject it and place it before
 * {@code UsernamePasswordAuthenticationFilter} in the filter chain.</p>
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee a single execution
 * per request, even when the request is forwarded or included internally.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

   
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (token != null && jwtUtil.isTokenValid(token)) {
            authenticateRequest(token, request);
        }
       
        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private void authenticateRequest(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtUtil.extractClaims(token);

            UUID   userId = jwtUtil.extractUserId(claims);
            String email  = jwtUtil.extractEmail(claims);
            String role   = jwtUtil.extractRole(claims);

            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role));

            
            AuthenticatedUser principal = new AuthenticatedUser(userId, email, role);

            // Three-argument constructor signals a *fully authenticated* token
            // (credentials = null because we don't store passwords post-auth).
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            // Attaches extra metadata (remote address, session ID) to the auth object.
            // Not required for JWT flows, but helps Spring Security's audit trail.
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Store in the thread-local SecurityContext. Spring Security's downstream
            // filters and @PreAuthorize checks will read from here.
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("Authenticated userId={} role={} path={}",
                    userId, role, request.getRequestURI());

        } catch (Exception e) {
            // If claims extraction fails for any reason after isTokenValid() passed
            // (e.g. a race condition on expiry within the same millisecond), we
            // clear the context and let the request proceed as unauthenticated.
            log.warn("Failed to set authentication from JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
}
