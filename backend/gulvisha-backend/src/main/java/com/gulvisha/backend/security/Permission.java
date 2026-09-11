package com.gulvisha.backend.security;

/**
 * System permissions representing granular actions users can perform.
 * These are mapped to roles and used for authorization checks.
 */
public enum Permission {
    // Enquiry permissions
    ENQUIRY_VIEW("enquiry:view"),
    ENQUIRY_CREATE("enquiry:create"),
    ENQUIRY_UPDATE("enquiry:update"),
    ENQUIRY_DELETE("enquiry:delete"),

    // Lead permissions
    LEAD_VIEW("lead:view"),
    LEAD_CREATE("lead:create"),
    LEAD_UPDATE("lead:update"),
    LEAD_DELETE("lead:delete"),

    // Client permissions
    CLIENT_VIEW("client:view"),
    CLIENT_CREATE("client:create"),
    CLIENT_UPDATE("client:update"),
    CLIENT_DELETE("client:delete"),

    // Project permissions
    PROJECT_VIEW("project:view"),
    PROJECT_CREATE("project:create"),
    PROJECT_UPDATE("project:update"),
    PROJECT_DELETE("project:delete"),

    // Task permissions
    TASK_VIEW("task:view"),
    TASK_CREATE("task:create"),
    TASK_UPDATE("task:update"),
    TASK_DELETE("task:delete"),

    // Document permissions
    DOCUMENT_VIEW("document:view"),
    DOCUMENT_CREATE("document:create"),
    DOCUMENT_UPDATE("document:update"),
    DOCUMENT_DELETE("document:delete"),

    // Ticket permissions
    TICKET_VIEW("ticket:view"),
    TICKET_CREATE("ticket:create"),
    TICKET_UPDATE("ticket:update"),
    TICKET_DELETE("ticket:delete"),

    // Resource permissions
    RESOURCE_VIEW("resource:view"),
    RESOURCE_CREATE("resource:create"),
    RESOURCE_UPDATE("resource:update"),
    RESOURCE_DELETE("resource:delete"),

    // AI permissions
    AI_USE("ai:use"),
    AI_AGENT_EXECUTE("ai:agent:execute"),

    // Organization settings
    ORGANIZATION_SETTINGS("organization:settings"),

    // Users management
    USER_MANAGE("user:manage"),

    // Workflow permissions
    WORKFLOW_VIEW("workflow:view"),
    WORKFLOW_MANAGE("workflow:manage");

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}