package com.gulvisha.backend.agent.tool;

import com.gulvisha.backend.agent.AgentTool;
import com.gulvisha.backend.crm.Lead;
import com.gulvisha.backend.crm.LeadRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Reads one lead from the agent's own organization.
 * Args: {id: lead UUID} or {email: exact email}.
 */
@Component
public class GetLeadTool implements AgentTool {

    private final LeadRepository leadRepository;

    public GetLeadTool(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Override
    public String name() { return "get_lead"; }

    @Override
    public String description() {
        return "Read a single lead from this organization's CRM by id or email. Returns contact + stage only.";
    }

    @Override
    public String argumentSpec() { return "{ \"id\": \"optional lead UUID\", \"email\": \"optional exact email\" }"; }

    @Override
    public String requiredPermission() { return "lead:view"; }

    @Override
    public AgentToolResult execute(UUID organizationId, Map<String, Object> args) {
        Object id = args.get("id");
        if (id != null && !id.toString().isBlank()) {
            UUID leadId;
            try {
                leadId = UUID.fromString(id.toString().trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid lead id");
            }
            Lead lead = leadRepository.findById(leadId)
                    .filter(l -> l.getOrganizationId().equals(organizationId))
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
            return AgentToolResult.success(describe(lead));
        }
        Object email = args.get("email");
        if (email != null && !email.toString().isBlank()) {
            String wanted = email.toString().trim();
            Lead lead = leadRepository.findAll().stream()
                    .filter(l -> l.getOrganizationId().equals(organizationId)
                            && wanted.equalsIgnoreCase(l.getEmail()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
            return AgentToolResult.success(describe(lead));
        }
        throw new IllegalArgumentException("Provide 'id' or 'email'");
    }

    private static String describe(Lead lead) {
        return "Lead " + lead.getId() + ": " + lead.getFirstName()
                + (lead.getLastName() != null ? " " + lead.getLastName() : "")
                + " <" + lead.getEmail() + "> stage=" + lead.getStatus();
    }
}
