package com.gulvisha.backend.controller;

import com.gulvisha.backend.dto.AdminQuoteRequestResponse;
import com.gulvisha.backend.dto.AdminAuditResponse;
import com.gulvisha.backend.service.QuoteRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/admin/quote-requests")
public class AdminQuoteRequestController {

    private final QuoteRequestService quoteRequestService;

    public AdminQuoteRequestController(QuoteRequestService quoteRequestService) {
        this.quoteRequestService = quoteRequestService;
    }

    @GetMapping
    public Page<AdminQuoteRequestResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 100);
        if (from != null || to != null) {
            Instant fromDate = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant toDate = to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            return quoteRequestService.filter(PageRequest.of(safePage, safeSize), status, search, fromDate, toDate);
        }
        if (search != null && !search.isBlank()) {
            return quoteRequestService.search(PageRequest.of(safePage, safeSize), status, search);
        }

        if (status == null || status.isBlank()) {
            return quoteRequestService.findAll(PageRequest.of(safePage, safeSize));
        }

        return quoteRequestService.findAll(PageRequest.of(safePage, safeSize), status);
    }

    @PatchMapping("/{id}/status")
    public AdminQuoteRequestResponse updateStatus(
            @PathVariable UUID id,
            @RequestBody StatusUpdateRequest request
    ) {
        return quoteRequestService.updateStatus(id, request.status());
    }

    @PatchMapping("/{id}/notes")
    public AdminQuoteRequestResponse updateNotes(
            @PathVariable UUID id,
            @RequestBody NotesUpdateRequest request
    ) {
        return quoteRequestService.updateInternalNotes(id, request.internalNotes());
    }

    @GetMapping("/{id}/audit")
    public List<AdminAuditResponse> audit(@PathVariable UUID id) {
        return quoteRequestService.getAuditHistory(id);
    }

    @PatchMapping("/bulk-status")
    public List<AdminQuoteRequestResponse> updateStatuses(@RequestBody BulkStatusUpdateRequest request) {
        return quoteRequestService.updateStatuses(request.ids(), request.status());
    }

    public record StatusUpdateRequest(String status) {
    }

    public record NotesUpdateRequest(String internalNotes) {
    }

    public record BulkStatusUpdateRequest(List<UUID> ids, String status) {
    }
}