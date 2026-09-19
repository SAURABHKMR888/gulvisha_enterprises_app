package com.gulvisha.backend.agent.tool;

import com.gulvisha.backend.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Queues an outbound email for human approval. The agent NEVER sends
 * directly: execution creates an AgentApproval and the run pauses
 * (Phase 13 §7). A separate delivery path sends only after APPROVED.
 * Args: {to: email, subject: string, body: string}.
 */
@Component
public class SendEmailTool implements AgentTool {

    @Override
    public String name() { return "send_email"; }

    @Override
    public String description() {
        return "Propose an outbound email. Requires human approval before anything is sent.";
    }

    @Override
    public String argumentSpec() {
        return "{ \"to\": \"required email\", \"subject\": \"required\", \"body\": \"required\" }";
    }

    @Override
    public String requiredPermission() { return "enquiry:create"; }

    @Override
    public boolean requiresApproval() { return true; }

    @Override
    public AgentToolResult execute(UUID organizationId, Map<String, Object> args) {
        Object to = args.get("to");
        Object subject = args.get("subject");
        Object body = args.get("body");
        if (to == null || to.toString().isBlank()) throw new IllegalArgumentException("'to' is required");
        if (subject == null || subject.toString().isBlank()) throw new IllegalArgumentException("'subject' is required");
        if (body == null || body.toString().isBlank()) throw new IllegalArgumentException("'body' is required");
        return AgentToolResult.success("Email proposal queued for approval: to=" + to + " subject=" + subject);
    }
}
