package com.gulvisha.backend.workflow;

import com.gulvisha.backend.entity.QuoteRequestEntity;
import com.gulvisha.backend.repository.QuoteRequestRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Appends a configurable note to the enquiry's internal notes.
 * The note text comes from the WorkflowStep.config field.
 */
@Component
public class AddNoteAction implements WorkflowAction {

    private final QuoteRequestRepository quoteRequestRepository;

    public AddNoteAction(QuoteRequestRepository quoteRequestRepository) {
        this.quoteRequestRepository = quoteRequestRepository;
    }

    @Override
    public String type() {
        return "ADD_NOTE";
    }

    @Override
    public String execute(UUID organizationId, UUID entityId, String config) {
        QuoteRequestEntity enquiry = quoteRequestRepository.findById(entityId)
                .filter(e -> e.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new IllegalArgumentException("Enquiry not found: " + entityId));

        String note = config == null || config.isBlank() ? "Processed by workflow" : config.trim();
        enquiry.setInternalNotes(enquiry.getInternalNotes() == null || enquiry.getInternalNotes().isBlank()
                ? note
                : enquiry.getInternalNotes() + " | " + note);
        quoteRequestRepository.save(enquiry);
        return "Added note to enquiry " + enquiry.getId();
    }
}
