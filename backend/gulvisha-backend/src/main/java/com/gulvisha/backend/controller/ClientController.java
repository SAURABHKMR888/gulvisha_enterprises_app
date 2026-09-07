package com.gulvisha.backend.controller;

import com.gulvisha.backend.crm.Client;
import com.gulvisha.backend.crm.ClientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
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
    public ResponseEntity<Client> create(@RequestBody ClientService.ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(request));
    }

    @PutMapping("/{id}")
    public Client update(@PathVariable UUID id, @RequestBody ClientService.ClientRequest request) {
        return clientService.updateClient(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
