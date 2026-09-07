package com.gulvisha.backend.ai.provider.impl;

import com.gulvisha.backend.ai.provider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OllamaAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiProvider.class);

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String url = (request.baseUrl() != null ? request.baseUrl() : "http://localhost:11434") + "/api/chat";

        List<Map<String, String>> messages = request.messages().stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList();

        Map<String, Object> body = Map.of(
                "model", request.model(),
                "messages", messages,
                "stream", false,
                "options", Map.of(
                        "temperature", request.temperature() != null ? request.temperature() : 0.7,
                        "num_predict", request.maxTokens() != null ? request.maxTokens() : 2048
                )
        );

        try {
            Map<?, ?> response = new RestTemplate().postForObject(url, body, Map.class);
            if (response == null) return new AiChatResponse("No response from model", request.model(), 0);
            Map<?, ?> message = (Map<?, ?>) response.get("message");
            String content = message != null ? (String) message.get("content") : "";
            Integer tokens = response.get("eval_count") != null ? ((Number) response.get("eval_count")).intValue() : null;
            return new AiChatResponse(content, request.model(), tokens);
        } catch (Exception e) {
            log.error("Ollama chat failed: {}", e.getMessage());
            return new AiChatResponse("Error: " + e.getMessage(), request.model(), 0);
        }
    }
}
