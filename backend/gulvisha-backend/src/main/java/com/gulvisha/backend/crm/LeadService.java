package com.gulvisha.backend.crm;

import com.gulvisha.backend.organization.Organization;
import com.gulvisha.backend.organization.OrganizationRepository;
import com.gulvisha.backend.pipeline.PipelineStageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LeadService {
    private final LeadRepository leadRepository;
    private final ClientRepository clientRepository;
    private final OrganizationRepository organizationRepository;
    private final PipelineStageRepository pipelineStageRepository;

    public LeadService(LeadRepository leadRepository,
                       ClientRepository clientRepository,
                       OrganizationRepository organizationRepository,
                       PipelineStageRepository pipelineStageRepository) {
        this.leadRepository = leadRepository;
        this.clientRepository = clientRepository;
        this.organizationRepository = organizationRepository;
        this.pipelineStageRepository = pipelineStageRepository;
    }

    private UUID getDefaultOrgId() {
        return organizationRepository.findAll().stream()
                .findFirst()
                .map(Organization::getId)
                .orElseThrow(() -> new IllegalStateException("No organization configured"));
    }

    public List<Lead> getAllLeads() {
        return leadRepository.findAllByOrganizationIdOrderByCreatedAtDesc(getDefaultOrgId());
    }

    public Page<Lead> getLeads(Pageable pageable, String status, String search) {
        UUID orgId = getDefaultOrgId();
        Specification<Lead> spec = Specification.where((root, query, cb) ->
                cb.equal(root.get("organizationId"), orgId));

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("lastName")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("company")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("email")), "%" + search.toLowerCase() + "%")
            ));
        }

        return leadRepository.findAll(spec, pageable);
    }

    public Lead getLeadById(UUID id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + id));
        if (!lead.getOrganizationId().equals(getDefaultOrgId())) {
            // Tenant isolation: never reveal that a lead exists in another organization
            throw new IllegalArgumentException("Lead not found: " + id);
        }
        return lead;
    }

    public Lead createLead(LeadRequest request) {
        Lead lead = new Lead(
                getDefaultOrgId(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.company(),
                request.service()
        );
        lead.setSource(request.source());
        lead.setLeadScore(request.leadScore());

        // Set default status
        List<com.gulvisha.backend.pipeline.PipelineStage> stages =
                pipelineStageRepository.findAllByOrganizationIdOrderBySortOrder(getDefaultOrgId());
        if (!stages.isEmpty()) {
            lead.setStatus(stages.get(0).getName());
        }

        Lead saved = leadRepository.save(lead);

        // If sourceId is set, try to link to an enquiry/quote
        if (request.sourceId() != null) {
            lead.setSourceId(request.sourceId());
            leadRepository.save(lead);
        }

        return saved;
    }

    public Lead updateLead(UUID id, LeadRequest request) {
        Lead lead = getLeadById(id);
        lead.setFirstName(request.firstName());
        lead.setLastName(request.lastName());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setCompany(request.company());
        lead.setService(request.service());
        lead.setSource(request.source());
        lead.setLeadScore(request.leadScore());
        lead.setNotes(request.notes());
        return leadRepository.save(lead);
    }

    public Lead updateStatus(UUID id, String status) {
        Lead lead = getLeadById(id);
        lead.setStatus(status);
        return leadRepository.save(lead);
    }

    public void deleteLead(UUID id) {
        Lead lead = getLeadById(id);
        leadRepository.delete(lead);
    }

    @Transactional
    public Lead convertToClient(UUID id, ClientRequest clientRequest) {
        Lead lead = getLeadById(id);

        // Create client from lead data (lead history preserved in notes)
        Client client = new Client(
                lead.getOrganizationId(),
                lead.getCompany() != null && !lead.getCompany().isBlank()
                        ? lead.getCompany()
                        : lead.getFirstName() + " " + (lead.getLastName() != null ? lead.getLastName() : ""),
                lead.getFirstName(),
                lead.getLastName(),
                lead.getEmail(),
                lead.getPhone(),
                clientRequest.industry(),
                clientRequest.address()
        );
        client.setAssignedManager(clientRequest.assignedManager());
        clientRepository.save(client);

        // Update lead to mark as converted (history preserved via sourceId link)
        lead.setStatus("CLIENT");
        lead.setSourceId("client:" + client.getId());
        leadRepository.save(lead);

        return lead;
    }

    public record LeadRequest(
            String firstName,
            String lastName,
            String email,
            String phone,
            String company,
            String service,
            String source,
            Integer leadScore,
            String sourceId,
            String notes
    ) {}

    public record ClientRequest(
            String industry,
            String address,
            String assignedManager
    ) {}
}