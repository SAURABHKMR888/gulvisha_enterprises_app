package com.gulvisha.backend.ai.provider.impl;

import com.gulvisha.backend.ai.provider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class GeminiAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String model = request.model() != null ? request.model() : "gemini-1.5-flash";
        String baseUrl = request.baseUrl() != null ? request.baseUrl() : "https://generativelanguage.googleapis.com";
        String url = baseUrl + "/v1beta/models/" + model + ":generateContent?key=" + (request.apiKey() != null ? request.apiKey() : "");

        List<Map<String, Object>> contents = request.messages().stream()
                .map(m -> Map.of("role", "system".equals(m.role()) ? "user" : m.role(),
                        "parts", List.of(Map.of("text", m.content()))))
                .toList();

        Map<String, Object> body = Map.of(
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", request.temperature() != null ? request.temperature() : 0.7,
                        "maxOutputTokens", request.maxTokens() != null ? request.maxTokens() : 2048
                )
        );

        try {
            Map<?, ?> response = new RestTemplate().postForObject(url, body, Map.class);
            if (response == null) return new AiChatResponse("No response from Gemini", model, 0);
            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return new AiChatResponse("No candidates", model, 0);
            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) candidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            String text = (String) ((Map<?, ?>) parts.get(0)).get("text");
            return new AiChatResponse(text, model, null);
        } catch (Exception e) {
            log.error("Gemini chat failed: {}", e.getMessage());
            return new AiChatResponse("Error: " + e.getMessage(), model, 0);
        }
    }

    @Override
    public String defaultEmbeddingModel() {
        return "gemini-embedding-001";
    }

    @Override
    public AiEmbeddingResponse embed(AiEmbeddingRequest request) {
        String embModel = request.model() != null && !request.model().isBlank()
                ? request.model() : defaultEmbeddingModel();
        String baseUrl = request.baseUrl() != null && !request.baseUrl().isBlank()
                ? request.baseUrl() : "https://generativelanguage.googleapis.com";
        String url = baseUrl + "/v1beta/models/" + embModel + ":embedContent?key="
                + (request.apiKey() != null ? request.apiKey() : "");

        List<float[]> vectors = new java.util.ArrayList<>();
        try {
            for (String input : request.inputs()) {
                Map<String, Object> body = Map.of("content", Map.of("parts", List.of(Map.of("text", input))));
                Map<?, ?> response = new RestTemplate().postForObject(url, body, Map.class);
                if (response == null || response.get("embedding") == null) {
                    throw new IllegalStateException("Gemini returned no embedding for model " + embModel);
                }
                Map<?, ?> embedding = (Map<?, ?>) response.get("embedding");
                List<?> values = (List<?>) embedding.get("values");
                float[] vector = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    vector[i] = ((Number) values.get(i)).floatValue();
                }
                vectors.add(vector);
            }
            return new AiEmbeddingResponse(vectors, embModel);
        } catch (Exception e) {
            log.error("Gemini embedding failed: {}", e.getMessage());
            throw new IllegalStateException("Gemini embedding failed: " + e.getMessage(), e);
        }
    }
}
