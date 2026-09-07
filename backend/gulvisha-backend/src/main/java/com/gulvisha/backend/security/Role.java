package com.gulvisha.backend.security;

/**
 * System roles defining access levels for users across the platform.
 * Roles are hierarchical within an organization:
 * PLATFORM_ADMIN > ORGANIZATION_ADMIN > MANAGER > EMPLOYEE > AGENT > CLIENT
 */
public enum Role {
    PLATFORM_ADMIN,       // Full platform access
    ORGANIZATION_ADMIN,   // Full access within organization
    MANAGER,              // Management-level access
    EMPLOYEE,             // Standard employee access
    AGENT,                // AI agent access
    CLIENT                // Client portal access
}