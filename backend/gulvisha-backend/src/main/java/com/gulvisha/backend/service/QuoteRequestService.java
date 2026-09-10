package com.gulvisha.backend.service;

import com.gulvisha.backend.dto.*;
import com.gulvisha.backend.entity.*;
import com.gulvisha.backend.repository.*;
import com.gulvisha.backend.organization.*;
import com.gulvisha.backend.security.UserContext;
import com.gulvisha.backend.workflow.WorkflowEngine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class QuoteRequestService {

    private final QuoteRequestRepository quoteRequestRepository;
    private final EnquiryAuditRepository enquiryAuditRepository;
    private final EnquiryNotificationService enquiryNotificationService;
    private final OrganizationRepository organizationRepository;
    private final WorkflowEngine workflowEngine;

    public QuoteRequestService(QuoteRequestRepository quoteRequestRepository,
                               EnquiryAuditRepository enquiryAuditRepository,
                               EnquiryNotificationService enquiryNotificationService,
                               OrganizationRepository organizationRepository,
                               WorkflowEngine workflowEngine) {
        this.quoteRequestRepository = quoteRequestRepository;
        this.enquiryAuditRepository = enquiryAuditRepository;
        this.enquiryNotificationService = enquiryNotificationService;
        this.organizationRepository = organizationRepository;
        this.workflowEngine = workflowEngine;
    }

    private UUID resolveOrganizationId(String slug) {
        if (slug != null && !slug.isBlank()) {
            return organizationRepository.findBySlug(slug)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown tenant: " + slug))
                    .getId();
        }
        // Fallback for single-tenant mode: first org
        return organizationRepository.findAll().stream()
                .findFirst()
                .map(Organization::getId)
                .orElseThrow(() -> new IllegalStateException("No organization configured"));
    }

    private UUID getAuthenticatedOrganizationId() {
        return UserContext.getOrganizationId();
    }

    private AdminQuoteRequestResponse toAdminResponse(QuoteRequestEntity request) {
        return new AdminQuoteRequestResponse(
                request.getId(), request.getName(), request.getEmail(), request.getCompany(),
                request.getService(), request.getPhone(), request.getServiceCategory(), request.getBudget(),
                request.getTimeline(), request.getSource(), request.getPreferredContactMethod(),
                request.getDetails(), request.getStatus(), request.getInternalNotes(), request.getReceivedAt()
        );
    }

    public QuoteRequestResponse submit(QuoteRequest request, String slug) {
        UUID organizationId = resolveOrganizationId(slug);
        QuoteRequestEntity savedRequest = quoteRequestRepository.save(new QuoteRequestEntity(
                organizationId,
                request.name().trim(), request.email().trim().toLowerCase(), blankToNull(request.company()),
                request.service().trim(), request.details().trim()
        ));
        recordAudit(savedRequest.getId(), "ENQUIRY_CREATED", null, "NEW");
        enquiryNotificationService.notifyNewEnquiry(savedRequest);
        workflowEngine.executeForTrigger(organizationId, "ENQUIRY_CREATED", savedRequest.getId());
        return new QuoteRequestResponse(savedRequest.getId(), savedRequest.getReceivedAt(),
                "Thanks — your quote request has been received. We'll be in touch shortly.");
    }

    public QuoteRequestResponse submitEnquiry(EnquiryRequest request) {
        UUID organizationId = getAuthenticatedOrganizationId();
        QuoteRequestEntity savedRequest = quoteRequestRepository.save(new QuoteRequestEntity(
                organizationId,
                request.name().trim(), request.email().trim().toLowerCase(), blankToNull(request.companyName()),
                request.service().trim(), request.message().trim(), blankToNull(request.phone()),
                request.serviceCategory().trim(), blankToNull(request.budget()), blankToNull(request.timeline()),
                request.source() == null || request.source().isBlank() ? "WEBSITE" : request.source().trim().toUpperCase(),
                blankToNull(request.preferredContactMethod())
        ));
        recordAudit(savedRequest.getId(), "ENQUIRY_CREATED", null, "NEW");
        enquiryNotificationService.notifyNewEnquiry(savedRequest);
        workflowEngine.executeForTrigger(organizationId, "ENQUIRY_CREATED", savedRequest.getId());
        return new QuoteRequestResponse(savedRequest.getId(), savedRequest.getReceivedAt(),
                "Thanks — your enquiry has been received. We'll be in touch shortly.");
    }

    public Page<AdminQuoteRequestResponse> findAll(Pageable pageable) {
        return quoteRequestRepository.findAllByOrganizationIdOrderByReceivedAtDesc(getAuthenticatedOrganizationId(), pageable)
                .map(this::toAdminResponse);
    }

    public Page<AdminQuoteRequestResponse> findAll(Pageable pageable, String status) {
        String normalizedStatus = normalizeStatus(status);
        return quoteRequestRepository.findAllByOrganizationIdAndStatusOrderByReceivedAtDesc(getAuthenticatedOrganizationId(), normalizedStatus, pageable)
                .map(this::toAdminResponse);
    }

    public Page<AdminQuoteRequestResponse> search(Pageable pageable, String status, String search) {
        String normalizedStatus = status == null || status.isBlank() ? null : normalizeStatus(status);
        return quoteRequestRepository.search(getAuthenticatedOrganizationId(), normalizedStatus, search.trim(), pageable)
                .map(this::toAdminResponse);
    }

    public Page<AdminQuoteRequestResponse> filter(Pageable pageable, String status, String search,
                                                  Instant fromDate, Instant toDate) {
        String normalizedStatus = status == null || status.isBlank() ? null : normalizeStatus(status);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return quoteRequestRepository.filter(getAuthenticatedOrganizationId(), normalizedStatus, normalizedSearch, fromDate, toDate, pageable)
                .map(this::toAdminResponse);
    }

    public AdminQuoteRequestResponse updateStatus(UUID enquiryId, String status) {
        QuoteRequestEntity request = quoteRequestRepository.findById(enquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Enquiry not found: " + enquiryId));
        String normalizedStatus = normalizeStatus(status);
        String previousStatus = request.getStatus();
        request.setStatus(normalizedStatus);
        QuoteRequestEntity savedRequest = quoteRequestRepository.save(request);
        if (!normalizedStatus.equals(previousStatus)) {
            recordAudit(savedRequest.getId(), "STATUS_CHANGED", previousStatus, normalizedStatus);
        }
        return toAdminResponse(savedRequest);
    }

    public AdminQuoteRequestResponse updateInternalNotes(UUID enquiryId, String internalNotes) {
        QuoteRequestEntity request = quoteRequestRepository.findById(enquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Enquiry not found: " + enquiryId));
        request.setInternalNotes(internalNotes);
        QuoteRequestEntity savedRequest = quoteRequestRepository.save(request);
        recordAudit(savedRequest.getId(), "NOTES_UPDATED", null, internalNotes);
        return toAdminResponse(savedRequest);
    }

    public List<AdminQuoteRequestResponse> updateStatuses(List<UUID> enquiryIds, String status) {
        String normalizedStatus = normalizeStatus(status);
        List<QuoteRequestEntity> requests = quoteRequestRepository.findAllById(enquiryIds);
        requests.forEach(request -> {
            String previousStatus = request.getStatus();
            request.setStatus(normalizedStatus);
            if (!normalizedStatus.equals(previousStatus)) {
                recordAudit(request.getId(), "STATUS_CHANGED", previousStatus, normalizedStatus);
            }
        });
        return quoteRequestRepository.saveAll(requests).stream()
                .map(this::toAdminResponse)
                .toList();
    }

    public List<AdminAuditResponse> getAuditHistory(UUID enquiryId) {
        return enquiryAuditRepository.findAllByEnquiryIdOrderByCreatedAtDesc(enquiryId).stream()
                .map(audit -> new AdminAuditResponse(
                        audit.getId(), audit.getEnquiryId(), audit.getAction(), audit.getPreviousValue(),
                        audit.getNewValue(), audit.getActor(), audit.getCreatedAt()
                ))
                .toList();
    }

    private void recordAudit(UUID enquiryId, String action, String previousValue, String newValue) {
        enquiryAuditRepository.save(new EnquiryAuditEntity(enquiryId, action, previousValue, newValue, "admin"));
    }

    public AdminDashboardResponse getDashboardSummary() {
        UUID orgId = getAuthenticatedOrganizationId();
        return new AdminDashboardResponse(
                quoteRequestRepository.countByOrganizationIdAndStatus(orgId, null),
                quoteRequestRepository.countByOrganizationIdAndStatus(orgId, "NEW"),
                quoteRequestRepository.countByOrganizationIdAndStatus(orgId, "QUALIFIED"),
                quoteRequestRepository.countByOrganizationIdAndStatus(orgId, "IN_PROGRESS"),
                quoteRequestRepository.countByOrganizationIdAndStatus(orgId, "CLOSED"),
                quoteRequestRepository.countByOrganizationIdAndStatus(orgId, "REJECTED"),
                quoteRequestRepository.countByOrganizationIdAndStatus(orgId, "ARCHIVED")
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "NEW", "QUALIFIED", "IN_PROGRESS", "CLOSED", "REJECTED", "ARCHIVED" -> normalized;
            default -> throw new IllegalArgumentException("Unsupported status: " + status);
        };
    }
}
