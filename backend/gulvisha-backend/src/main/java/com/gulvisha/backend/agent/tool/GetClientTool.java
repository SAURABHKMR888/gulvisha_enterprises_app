package com.gulvisha.backend.agent.tool;

import com.gulvisha.backend.agent.AgentTool;
import com.gulvisha.backend.crm.Client;
import com.gulvisha.backend.crm.ClientRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Reads one client from the agent's own organization.
 * Args: {id: client UUID} or {email: exact email}.
 */
@Component
public class GetClientTool implements AgentTool {

    private final ClientRepository clientRepository;

    public GetClientTool(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public String name() { return "get_client"; }

    @Override
    public String description() {
        return "Read a single client from this organization by id or email.";
    }

    @Override
    public String argumentSpec() { return "{ \"id\": \"optional client UUID\", \"email\": \"optional exact email\" }"; }

    @Override
    public String requiredPermission() { return "client:view"; }

    @Override
    public AgentToolResult execute(UUID organizationId, Map<String, Object> args) {
        Object id = args.get("id");
        if (id != null && !id.toString().isBlank()) {
            UUID clientId;
            try {
                clientId = UUID.fromString(id.toString().trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid client id");
            }
            Client client = clientRepository.findById(clientId)
                    .filter(c -> c.getOrganizationId().equals(organizationId))
                    .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            return AgentToolResult.success(describe(client));
        }
        Object email = args.get("email");
        if (email != null && !email.toString().isBlank()) {
            String wanted = email.toString().trim();
            Client client = clientRepository.findAllByOrganizationIdOrderByName(organizationId).stream()
                    .filter(c -> wanted.equalsIgnoreCase(c.getEmail()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            return AgentToolResult.success(describe(client));
        }
        throw new IllegalArgumentException("Provide 'id' or 'email'");
    }

    private static String describe(Client c) {
        return "Client " + c.getId() + ": " + c.getName() + " <" + c.getEmail() + "> status=" + c.getStatus();
    }
}
