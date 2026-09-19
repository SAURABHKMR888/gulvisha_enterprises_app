package com.gulvisha.backend.agent.tool;

import com.gulvisha.backend.agent.AgentTool;
import com.gulvisha.backend.ai.service.RagService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Searches the organization's knowledge base via the RAG pipeline.
 * Args: {query: string}
 */
@Component
public class SearchKnowledgeTool implements AgentTool {

    private final RagService ragService;

    public SearchKnowledgeTool(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public String name() { return "search_knowledge_base"; }

    @Override
    public String description() {
        return "Search the organization's knowledge base documents and return the most relevant excerpts.";
    }

    @Override
    public String argumentSpec() { return "{ \"query\": \"text to search for\" }"; }

    @Override
    public String requiredPermission() { return "ai:use"; }

    @Override
    public AgentToolResult execute(UUID organizationId, Map<String, Object> args) {
        Object query = args.get("query");
        if (query == null || query.toString().isBlank()) {
            throw new IllegalArgumentException("'query' is required");
        }
        var hits = ragService.retrieve(organizationId, query.toString(), 5);
        if (hits.isEmpty()) {
            return AgentToolResult.success("No relevant knowledge found for: " + query);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            var hit = hits.get(i);
            sb.append("[").append(i + 1).append("] (")
              .append(hit.documentTitle()).append(", score ")
              .append(String.format("%.2f", hit.score())).append(")\n")
              .append(hit.content()).append("\n\n");
        }
        return AgentToolResult.success(sb.toString().trim());
    }
}
