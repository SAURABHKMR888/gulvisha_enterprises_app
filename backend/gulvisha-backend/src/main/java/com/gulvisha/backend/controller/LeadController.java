package com.gulvisha.backend.controller;

import com.gulvisha.backend.crm.Lead;
import com.gulvisha.backend.crm.LeadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
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
    public ResponseEntity<Lead> create(@RequestBody LeadService.LeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createLead(request));
    }

    @GetMapping("/{id}")
    public Lead getById(@PathVariable UUID id) {
        return leadService.getLeadById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lead> update(@PathVariable UUID id, @RequestBody LeadService.LeadRequest request) {
        return ResponseEntity.ok(leadService.updateLead(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Lead> updateStatus(@PathVariable UUID id, @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(leadService.updateStatus(id, request.status()));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<Lead> convertToClient(@PathVariable UUID id, @RequestBody LeadService.ClientRequest request) {
        return ResponseEntity.ok(leadService.convertToClient(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        leadService.deleteLead(id);
        return ResponseEntity.noContent().build();
    }

    public record StatusUpdateRequest(String status) {}
}