package com.gulvisha.backend.controller;

import com.gulvisha.backend.crm.Lead;
import com.gulvisha.backend.crm.LeadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
@PreAuthorize("hasAnyAuthority('PERMISSION_lead:view', 'PERMISSION_lead:create', 'PERMISSION_lead:update', 'PERMISSION_lead:delete')")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public Page<Lead> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return leadService.getLeads(PageRequest.of(page, size), status, search);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_lead:create')")
    public ResponseEntity<Lead> create(@RequestBody LeadService.LeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createLead(request));
    }

    @GetMapping("/{id}")
    public Lead getById(@PathVariable UUID id) {
        return leadService.getLeadById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_lead:update')")
    public ResponseEntity<Lead> update(@PathVariable UUID id, @RequestBody LeadService.LeadRequest request) {
        return ResponseEntity.ok(leadService.updateLead(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERMISSION_lead:update')")
    public ResponseEntity<Lead> updateStatus(@PathVariable UUID id, @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(leadService.updateStatus(id, request.status()));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<Lead> convertToClient(@PathVariable UUID id, @RequestBody LeadService.ClientRequest request) {
        return ResponseEntity.ok(leadService.convertToClient(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_lead:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        leadService.deleteLead(id);
        return ResponseEntity.noContent().build();
    }

    public record StatusUpdateRequest(String status) {}
}