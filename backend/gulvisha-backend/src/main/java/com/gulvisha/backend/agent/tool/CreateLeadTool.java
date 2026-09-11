package com.gulvisha.backend.agent.tool;

import com.gulvisha.backend.agent.AgentTool;
import com.gulvisha.backend.crm.Lead;
import com.gulvisha.backend.crm.LeadRepository;
import com.gulvisha.backend.pipeline.PipelineStage;
import com.gulvisha.backend.pipeline.PipelineStageRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Creates a lead in the agent's own organization, placed in the tenant's
 * default pipeline stage.
 * Args: {firstName: string, email: string, lastName?: string, company?: string,
 *        phone?: string, service?: string, notes?: string}
 */
@Component
public class CreateLeadTool implements AgentTool {

    private final LeadRepository leadRepository;
    private final PipelineStageRepository pipelineStageRepository;

    public CreateLeadTool(LeadRepository leadRepository, PipelineStageRepository pipelineStageRepository) {
        this.leadRepository = leadRepository;
        this.pipelineStageRepository = pipelineStageRepository;
    }

    @Override
    public String name() { return "create_lead"; }

    @Override
    public String description() {
        return "Create a new lead (potential customer) in this organization's CRM, placed in the default pipeline stage.";
    }

    @Override
    public String argumentSpec() {
        return "{ \"firstName\": \"required\", \"email\": \"required\", \"lastName\": \"optional\", "
             + "\"company\": \"optional\", \"phone\": \"optional\", \"service\": \"optional\", \"notes\": \"optional\" }";
    }

    @Override
    public AgentToolResult execute(UUID organizationId, Map<String, Object> args) {
        Object firstName = args.get("firstName");
        Object email = args.get("email");
        if (firstName == null || firstName.toString().isBlank()) {
            throw new IllegalArgumentException("'firstName' is required");
        }
        if (email == null || email.toString().isBlank()) {
            throw new IllegalArgumentException("'email' is required");
        }
        Lead lead = new Lead(
                organizationId,
                firstName.toString().trim(),
                str(args, "lastName"),
                email.toString().trim(),
                str(args, "phone"),
                str(args, "company"),
                str(args, "service"));
        lead.setSource("agent");
        String notes = str(args, "notes");
        if (notes != null) lead.setNotes(notes);

        // Place in the tenant's default pipeline stage
        var stages = pipelineStageRepository.findAllByOrganizationIdOrderBySortOrder(organizationId);
        stages.stream().filter(PipelineStage::isDefaultStage).findFirst()
                .or(() -> stages.stream().findFirst())
                .ifPresent(stage -> lead.setStatus(stage.getName()));

        Lead saved = leadRepository.save(lead);
        return AgentToolResult.success("Lead created (id=" + saved.getId() + "): "
                + saved.getFirstName() + " <" + saved.getEmail() + "> in stage " + saved.getStatus());
    }

    private static String str(Map<String, Object> args, String key) {
        return args.get(key) != null && !args.get(key).toString().isBlank()
                ? args.get(key).toString().trim() : null;
    }
}
