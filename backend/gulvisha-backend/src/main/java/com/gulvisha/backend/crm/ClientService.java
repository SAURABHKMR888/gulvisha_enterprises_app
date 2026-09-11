package com.gulvisha.backend.crm;

import com.gulvisha.backend.security.UserContext;
import com.gulvisha.backend.user.User;
import com.gulvisha.backend.user.UserRepository;
import com.gulvisha.backend.security.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ClientService {
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientService(ClientRepository clientRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private UUID getOrgId() {
        UUID orgId = UserContext.getOrganizationId();
        if (orgId == null) {
            throw new IllegalStateException("No organization context");
        }
        return orgId;
    }

    public Page<Client> getClients(Pageable pageable, String search) {
        UUID orgId = getOrgId();
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
        if (!client.getOrganizationId().equals(getOrgId())) {
            // Tenant isolation: never reveal that a client exists in another organization
            throw new IllegalArgumentException("Client not found: " + id);
        }
        return client;
    }

    public Client createClient(ClientRequest request) {
        Client client = new Client(
                getOrgId(),
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

    /**
     * Provisions a CLIENT-role user linked to an existing client record.
     * The user can then sign in and access only that client's data via the portal.
     */
    public User createPortalUser(UUID clientId, ClientPortalUserRequest request) {
        Client client = getClientById(clientId);
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        User user = new User(
                client.getOrganizationId(),
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                Role.CLIENT
        );
        user.setClientId(client.getId());
        return userRepository.save(user);
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
