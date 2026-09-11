package com.gulvisha.backend.agent.tool;

import com.gulvisha.backend.agent.AgentTool;
import com.gulvisha.backend.service.Service;
import com.gulvisha.backend.service.ServiceRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Lists the tenant's configured services (name, category, description).
 * Args: none ({}).
 */
@Component
public class GetServicesTool implements AgentTool {

    private final ServiceRepository serviceRepository;

    public GetServicesTool(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    public String name() { return "get_services"; }

    @Override
    public String description() {
        return "List the services this organization offers, with category and description.";
    }

    @Override
    public String argumentSpec() { return "{}"; }

    @Override
    public AgentToolResult execute(UUID organizationId, Map<String, Object> args) {
        var services = serviceRepository.findAllByOrganizationIdOrderByDisplayOrderAscNameAsc(organizationId);
        if (services.isEmpty()) {
            return AgentToolResult.success("No services configured for this organization.");
        }
        StringBuilder sb = new StringBuilder("Services:\n");
        for (Service s : services) {
            sb.append("- ").append(s.getName());
            if (s.getCategory() != null && !s.getCategory().isBlank()) sb.append(" [").append(s.getCategory()).append("]");
            if (s.getDescription() != null && !s.getDescription().isBlank()) sb.append(": ").append(s.getDescription());
            sb.append("\n");
        }
        return AgentToolResult.success(sb.toString().trim());
    }
}
