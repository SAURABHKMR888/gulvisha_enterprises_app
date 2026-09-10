package com.gulvisha.backend.ai.provider;

import java.util.List;

/**
 * Request for generating embeddings for one or more text inputs.
 */
public record AiEmbeddingRequest(
    String model,
    List<String> inputs,
    String apiKey,
    String baseUrl
) {
    public static AiEmbeddingRequest of(String model, String input) {
        return new AiEmbeddingRequest(model, List.of(input), null, null);
    }
}
