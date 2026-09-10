package com.gulvisha.backend.ai.provider.impl;

import com.gulvisha.backend.ai.provider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAiProvider.class);

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String model = request.model() != null ? request.model() : "gpt-4o-mini";
        String baseUrl = request.baseUrl() != null ? request.baseUrl() : "https://api.openai.com";
        String url = baseUrl + "/v1/chat/completions";

        List<Map<String, String>> messages = request.messages().stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList();

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", request.temperature() != null ? request.temperature() : 0.7,
                "max_tokens", request.maxTokens() != null ? request.maxTokens() : 2048
        );

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (request.apiKey() != null && !request.apiKey().isBlank()) {
                headers.setBearerAuth(request.apiKey());
            }
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);
            if (response == null) return new AiChatResponse("No response from OpenAI", model, 0);
            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) return new AiChatResponse("No choices", model, 0);
            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            String content = (String) message.get("content");
            Integer tokens = null;
            if (response.get("usage") != null && ((Map<?, ?>) response.get("usage")).get("total_tokens") != null) {
                tokens = ((Number) ((Map<?, ?>) response.get("usage")).get("total_tokens")).intValue();
            }
            return new AiChatResponse(content, model, tokens);
        } catch (Exception e) {
            log.error("OpenAI chat failed: {}", e.getMessage());
            return new AiChatResponse("Error: " + e.getMessage(), model, 0);
        }
    }

    @Override
    public String defaultEmbeddingModel() {
        return "text-embedding-3-small";
    }

    @Override
    public AiEmbeddingResponse embed(AiEmbeddingRequest request) {
        String embModel = request.model() != null && !request.model().isBlank()
                ? request.model() : defaultEmbeddingModel();
        String baseUrl = request.baseUrl() != null && !request.baseUrl().isBlank()
                ? request.baseUrl() : "https://api.openai.com";
        String url = baseUrl + "/v1/embeddings";

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (request.apiKey() != null && !request.apiKey().isBlank()) {
                headers.setBearerAuth(request.apiKey());
            }
            Map<String, Object> body = Map.of("model", embModel, "input", request.inputs());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);
            if (response == null || response.get("data") == null) {
                throw new IllegalStateException("OpenAI returned no embedding data for model " + embModel);
            }
            List<?> data = (List<?>) response.get("data");
            List<float[]> vectors = new java.util.ArrayList<>();
            for (Object item : data) {
                Map<?, ?> entry = (Map<?, ?>) item;
                List<?> values = (List<?>) entry.get("embedding");
                float[] vector = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    vector[i] = ((Number) values.get(i)).floatValue();
                }
                vectors.add(vector);
            }
            return new AiEmbeddingResponse(vectors, embModel);
        } catch (Exception e) {
            log.error("OpenAI embedding failed: {}", e.getMessage());
            throw new IllegalStateException("OpenAI embedding failed: " + e.getMessage(), e);
        }
    }
}
