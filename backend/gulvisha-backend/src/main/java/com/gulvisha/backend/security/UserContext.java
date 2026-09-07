package com.gulvisha.backend.security;

import java.util.UUID;

/**
 * Thread-local holder for the authenticated user's context.
 * Populated by JwtAuthenticationFilter from JWT claims; cleared after each request.
 * clientId is only present for users with the CLIENT role (portal users).
 */
public final class UserContext {

    public record CurrentUser(String username, UUID organizationId, UUID clientId, String role) {}

    private static final ThreadLocal<CurrentUser> CONTEXT = new ThreadLocal<>();

    private UserContext() {}

    public static void set(CurrentUser user) {
        CONTEXT.set(user);
    }

    public static CurrentUser get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static UUID getOrganizationId() {
        CurrentUser user = CONTEXT.get();
        return user == null ? null : user.organizationId();
    }

    public static UUID getClientId() {
        CurrentUser user = CONTEXT.get();
        return user == null ? null : user.clientId();
    }
}