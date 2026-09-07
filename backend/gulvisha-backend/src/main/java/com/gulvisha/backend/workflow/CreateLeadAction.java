package com.gulvisha.backend.workflow;

import com.gulvisha.backend.crm.Lead;
import com.gulvisha.backend.crm.LeadRepository;
import com.gulvisha.backend.entity.QuoteRequestEntity;
import com.gulvisha.backend.repository.QuoteRequestRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Converts an enquiry into a CRM lead.
 * Idempotent: a lead is only created once per enquiry (tracked via sourceId = "enquiry:{id}").
 */
@Component
public class CreateLeadAction implements WorkflowAction {

    private final QuoteRequestRepository quoteRequestRepository;
    private final LeadRepository leadRepository;

    public CreateLeadAction(QuoteRequestRepository quoteRequestRepository, LeadRepository leadRepository) {
        this.quoteRequestRepository = quoteRequestRepository;
        this.leadRepository = leadRepository;
    }

    @Override
    public String type() {
        return "CREATE_LEAD";
    }

    @Override
    public String execute(UUID organizationId, UUID entityId, String config) {
        QuoteRequestEntity enquiry = quoteRequestRepository.findById(entityId)
                .filter(e -> e.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new IllegalArgumentException("Enquiry not found: " + entityId));

        String sourceId = "enquiry:" + enquiry.getId();
        Optional<Lead> existing = leadRepository.findByOrganizationIdAndSourceId(organizationId, sourceId);
        if (existing.isPresent()) {
            return "Lead already exists for enquiry " + enquiry.getId() + " — skipped";
        }

        Lead lead = new Lead(organizationId,
                enquiry.getName(), null, enquiry.getEmail(), enquiry.getPhone(),
                enquiry.getCompany(), enquiry.getService());
        lead.setSourceId(sourceId);
        lead.setSource(enquiry.getSource() != null ? enquiry.getSource() : "WEBSITE");
        lead.setNotes("Created from enquiry: " + enquiry.getDetails());
        Lead saved = leadRepository.save(lead);
        return "Created lead " + saved.getId() + " from enquiry " + enquiry.getId();
    }
}
