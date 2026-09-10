package com.gulvisha.backend.ai.provider;

import java.util.List;

/**
 * Response containing one embedding vector per requested input, in input order.
 */
public record AiEmbeddingResponse(
    List<float[]> embeddings,
    String model
) {}
