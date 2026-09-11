package com.gulvisha.backend.security;

import java.util.List;
import java.util.Map;

/**
 * Maps roles to their permitted actions.
 * Defines which permissions each role inherits.
 */
public class RolePermission {

    public static Map<Role, List<Permission>> ROLE_PERMISSIONS = Map.of(
        // Platform Admin: full access
        Role.PLATFORM_ADMIN, List.of(
            Permission.ENQUIRY_VIEW, Permission.ENQUIRY_CREATE, Permission.ENQUIRY_UPDATE, Permission.ENQUIRY_DELETE,
            Permission.LEAD_VIEW, Permission.LEAD_CREATE, Permission.LEAD_UPDATE, Permission.LEAD_DELETE,
            Permission.CLIENT_VIEW, Permission.CLIENT_CREATE, Permission.CLIENT_UPDATE, Permission.CLIENT_DELETE,
            Permission.PROJECT_VIEW, Permission.PROJECT_CREATE, Permission.PROJECT_UPDATE, Permission.PROJECT_DELETE,
            Permission.TASK_VIEW, Permission.TASK_CREATE, Permission.TASK_UPDATE, Permission.TASK_DELETE,
            Permission.DOCUMENT_VIEW, Permission.DOCUMENT_CREATE, Permission.DOCUMENT_UPDATE, Permission.DOCUMENT_DELETE,
            Permission.TICKET_VIEW, Permission.TICKET_CREATE, Permission.TICKET_UPDATE, Permission.TICKET_DELETE,
            Permission.RESOURCE_VIEW, Permission.RESOURCE_CREATE, Permission.RESOURCE_UPDATE, Permission.RESOURCE_DELETE,
            Permission.AI_USE, Permission.AI_AGENT_EXECUTE,
            Permission.ORGANIZATION_SETTINGS,
            Permission.USER_MANAGE,
            Permission.WORKFLOW_VIEW, Permission.WORKFLOW_MANAGE
        ),

        // Organization Admin: same as platform admin but org-scoped
        Role.ORGANIZATION_ADMIN, List.of(
            Permission.ENQUIRY_VIEW, Permission.ENQUIRY_CREATE, Permission.ENQUIRY_UPDATE, Permission.ENQUIRY_DELETE,
            Permission.LEAD_VIEW, Permission.LEAD_CREATE, Permission.LEAD_UPDATE, Permission.LEAD_DELETE,
            Permission.CLIENT_VIEW, Permission.CLIENT_CREATE, Permission.CLIENT_UPDATE, Permission.CLIENT_DELETE,
            Permission.PROJECT_VIEW, Permission.PROJECT_CREATE, Permission.PROJECT_UPDATE, Permission.PROJECT_DELETE,
            Permission.TASK_VIEW, Permission.TASK_CREATE, Permission.TASK_UPDATE, Permission.TASK_DELETE,
            Permission.DOCUMENT_VIEW, Permission.DOCUMENT_CREATE, Permission.DOCUMENT_UPDATE, Permission.DOCUMENT_DELETE,
            Permission.TICKET_VIEW, Permission.TICKET_CREATE, Permission.TICKET_UPDATE, Permission.TICKET_DELETE,
            Permission.AI_USE, Permission.AI_AGENT_EXECUTE,
            Permission.ORGANIZATION_SETTINGS,
            Permission.USER_MANAGE,
            Permission.WORKFLOW_VIEW, Permission.WORKFLOW_MANAGE
        ),

        // Manager: read/write most things, no AI agent execution
        Role.MANAGER, List.of(
            Permission.ENQUIRY_VIEW, Permission.ENQUIRY_CREATE, Permission.ENQUIRY_UPDATE,
            Permission.LEAD_VIEW, Permission.LEAD_CREATE, Permission.LEAD_UPDATE,
            Permission.CLIENT_VIEW, Permission.CLIENT_CREATE, Permission.CLIENT_UPDATE,
            Permission.PROJECT_VIEW, Permission.PROJECT_CREATE, Permission.PROJECT_UPDATE,
            Permission.TASK_VIEW, Permission.TASK_CREATE, Permission.TASK_UPDATE,
            Permission.DOCUMENT_VIEW, Permission.DOCUMENT_CREATE,
            Permission.TICKET_VIEW, Permission.TICKET_CREATE, Permission.TICKET_UPDATE,
            Permission.AI_USE,
            Permission.WORKFLOW_VIEW
        ),

        // Employee: read most things, write own entries
        Role.EMPLOYEE, List.of(
            Permission.ENQUIRY_VIEW,
            Permission.LEAD_VIEW, Permission.LEAD_CREATE,
            Permission.CLIENT_VIEW,
            Permission.PROJECT_VIEW, Permission.TASK_VIEW, Permission.TASK_UPDATE,
            Permission.DOCUMENT_VIEW,
            Permission.TICKET_VIEW, Permission.TICKET_CREATE
        ),

        // Agent: limited permissions
        Role.AGENT, List.of(
            Permission.LEAD_VIEW, Permission.LEAD_CREATE, Permission.LEAD_UPDATE,
            Permission.CLIENT_VIEW,
            Permission.PROJECT_VIEW,
            Permission.TASK_VIEW, Permission.TASK_CREATE, Permission.TASK_UPDATE,
            Permission.AI_USE, Permission.AI_AGENT_EXECUTE,
            Permission.DOCUMENT_VIEW
        ),

        // Client: very limited - own data only
        Role.CLIENT, List.of(
            Permission.PROJECT_VIEW,
            Permission.TASK_VIEW,
            Permission.DOCUMENT_VIEW,
            Permission.TICKET_VIEW, Permission.TICKET_CREATE
        )
    );

    public static List<Permission> getPermissionsForRole(Role role) {
        return ROLE_PERMISSIONS.getOrDefault(role, List.of());
    }
}