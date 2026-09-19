package com.gulvisha.backend.agent.tool;

import com.gulvisha.backend.agent.AgentTool;
import com.gulvisha.backend.crm.Lead;
import com.gulvisha.backend.crm.LeadRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Updates mutable fields of a lead in the agent's own organization.
 * High-impact: requires human approval via the guardrail layer.
 * Args: {id: lead UUID, status?: string, assignedTo?: string, notes?: string}.
 */
@Component
public class UpdateLeadTool implements AgentTool {

    private final LeadRepository leadRepository;

    public UpdateLeadTool(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Override
    public String name() { return "update_lead"; }

    @Override
    public String description() {
        return "Update a lead's stage, assignee or notes in this organization's CRM.";
    }

    @Override
    public String argumentSpec() {
        return "{ \"id\": \"required lead UUID\", \"status\": \"optional stage\", "
                + "\"assignedTo\": \"optional username\", \"notes\": \"optional\" }";
    }

    @Override
    public String requiredPermission() { return "lead:update"; }

    @Override
    public boolean requiresApproval() { return true; }

    @Override
    public AgentToolResult execute(UUID organizationId, Map<String, Object> args) {
        Object id = args.get("id");
        if (id == null || id.toString().isBlank()) {
            throw new IllegalArgumentException("'id' is required");
        }
        UUID leadId;
        try {
            leadId = UUID.fromString(id.toString().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid lead id");
        }
        Lead lead = leadRepository.findById(leadId)
                .filter(l -> l.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
        if (args.get("status") != null && !args.get("status").toString().isBlank()) {
            lead.setStatus(args.get("status").toString().trim());
        }
        if (args.get("assignedTo") != null && !args.get("assignedTo").toString().isBlank()) {
            lead.setAssignedTo(args.get("assignedTo").toString().trim());
        }
        if (args.get("notes") != null && !args.get("notes").toString().isBlank()) {
            lead.setNotes(args.get("notes").toString().trim());
        }
        leadRepository.save(lead);
        return AgentToolResult.success("Lead updated: " + lead.getId() + " stage=" + lead.getStatus());
    }
}
