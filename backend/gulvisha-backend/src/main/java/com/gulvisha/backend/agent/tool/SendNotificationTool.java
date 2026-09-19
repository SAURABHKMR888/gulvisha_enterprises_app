package com.gulvisha.backend.agent.tool;

import com.gulvisha.backend.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Queues an internal notification for human approval.
 * Args: {message: string, audience?: string}.
 */
@Component
public class SendNotificationTool implements AgentTool {

    @Override
    public String name() { return "send_notification"; }

    @Override
    public String description() {
        return "Propose an internal notification. Requires human approval before delivery.";
    }

    @Override
    public String argumentSpec() { return "{ \"message\": \"required\", \"audience\": \"optional\" }"; }

    @Override
    public String requiredPermission() { return "ai:use"; }

    @Override
    public boolean requiresApproval() { return true; }

    @Override
    public AgentToolResult execute(UUID organizationId, Map<String, Object> args) {
        Object message = args.get("message");
        if (message == null || message.toString().isBlank()) {
            throw new IllegalArgumentException("'message' is required");
        }
        return AgentToolResult.success("Notification proposal queued for approval: " + message.toString().trim());
    }
}
