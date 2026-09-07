package com.gulvisha.backend.crm;

import com.gulvisha.backend.organization.Organization;
import com.gulvisha.backend.organization.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ClientService {
    private final ClientRepository clientRepository;
    private final OrganizationRepository organizationRepository;

    public ClientService(ClientRepository clientRepository,
                         OrganizationRepository organizationRepository) {
        this.clientRepository = clientRepository;
        this.organizationRepository = organizationRepository;
    }

    private UUID getDefaultOrgId() {
        return organizationRepository.findAll().stream()
                .findFirst()
                .map(Organization::getId)
                .orElseThrow(() -> new IllegalStateException("No organization configured"));
    }

    public Page<Client> getClients(Pageable pageable, String search) {
        UUID orgId = getDefaultOrgId();
        Specification<Client> spec = Specification.where((root, query, cb) ->
                cb.equal(root.get("organizationId"), orgId));

        if (search != null && !search.isBlank()) {
            String term = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), term),
                    cb.like(cb.lower(root.get("email")), term),
                    cb.like(cb.lower(root.get("industry")), term)
            ));
        }

        return clientRepository.findAll(spec, pageable);
    }

    public Client getClientById(UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
        if (!client.getOrganizationId().equals(getDefaultOrgId())) {
            // Tenant isolation: never reveal that a client exists in another organization
            throw new IllegalArgumentException("Client not found: " + id);
        }
        return client;
    }

    public Client createClient(ClientRequest request) {
        Client client = new Client(
                getDefaultOrgId(),
                request.name(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.industry(),
                request.address()
        );
        client.setWebsite(request.website());
        client.setAssignedManager(request.assignedManager());
        if (request.status() != null && !request.status().isBlank()) {
            client.setStatus(request.status());
        }
        return clientRepository.save(client);
    }

    public Client updateClient(UUID id, ClientRequest request) {
        Client client = getClientById(id);
        client.setName(request.name());
        client.setFirstName(request.firstName());
        client.setLastName(request.lastName());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setIndustry(request.industry());
        client.setAddress(request.address());
        client.setWebsite(request.website());
        client.setAssignedManager(request.assignedManager());
        if (request.status() != null && !request.status().isBlank()) {
            client.setStatus(request.status());
        }
        return clientRepository.save(client);
    }

    public void deleteClient(UUID id) {
        Client client = getClientById(id);
        clientRepository.delete(client);
    }

    public record ClientRequest(
            String name,
            String firstName,
            String lastName,
            String email,
            String phone,
            String industry,
            String address,
            String website,
            String status,
            String assignedManager
    ) {}
}
