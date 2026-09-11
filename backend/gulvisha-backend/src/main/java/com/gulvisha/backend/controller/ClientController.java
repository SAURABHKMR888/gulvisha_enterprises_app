package com.gulvisha.backend.controller;

import com.gulvisha.backend.crm.Client;
import com.gulvisha.backend.crm.ClientPortalUserRequest;
import com.gulvisha.backend.crm.ClientService;
import com.gulvisha.backend.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@PreAuthorize("hasAnyAuthority('PERMISSION_client:view', 'PERMISSION_client:create', 'PERMISSION_client:update', 'PERMISSION_client:delete')")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public Page<Client> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return clientService.getClients(PageRequest.of(page, size), search);
    }

    @GetMapping("/{id}")
    public Client getById(@PathVariable UUID id) {
        return clientService.getClientById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_client:create')")
    public ResponseEntity<Client> create(@RequestBody ClientService.ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_client:update')")
    public Client update(@PathVariable UUID id, @RequestBody ClientService.ClientRequest request) {
        return clientService.updateClient(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_client:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Provisions a CLIENT-role portal user linked to this client.
     * The created user can sign in and access only this client's data via the portal.
     */
    @PostMapping("/{id}/portal-user")
    @PreAuthorize("hasAuthority('PERMISSION_user:manage')")
    public ResponseEntity<UserResponse> createPortalUser(@PathVariable UUID id, @RequestBody ClientPortalUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(clientService.createPortalUser(id, request)));
    }
}
