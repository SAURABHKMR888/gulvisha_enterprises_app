package com.gulvisha.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parseClaims(token);
                String username = claims.getSubject();

                @SuppressWarnings("unchecked")
                List<String> permissions = claims.get("permissions", List.class);

                if (permissions == null) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = claims.get("roles", List.class);
                    if (roles != null) {
                        permissions = roles;
                    }
                }

                if (username != null && permissions != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                    permissions.stream()
                            .map(perm -> perm.startsWith("PERMISSION_") ? perm : "PERMISSION_" + perm)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                    // Also expose the role as a raw authority (e.g. PLATFORM_ADMIN) so hasAuthority("PLATFORM_ADMIN") works
                    Object authRole = claims.get("role");
                    if (authRole != null && !authRole.toString().isBlank()) {
                        authorities.add(new SimpleGrantedAuthority(authRole.toString()));
                    }
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(username, null, authorities));

                    UUID organizationId = parseUuid(claims.get("organizationId"));
                    UUID clientId = parseUuid(claims.get("clientId"));
                    Object roleClaim = claims.get("role");
                    UserContext.set(new UserContext.CurrentUser(
                            username,
                            organizationId,
                            clientId,
                            roleClaim != null ? roleClaim.toString() : null));
                }
            } catch (Exception ignored) {
                // Invalid token — leave context unauthenticated
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Never leak user context to the next request on this pooled thread
            UserContext.clear();
        }
    }

    private UUID parseUuid(Object value) {
        if (value instanceof String s && !s.isBlank()) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}
