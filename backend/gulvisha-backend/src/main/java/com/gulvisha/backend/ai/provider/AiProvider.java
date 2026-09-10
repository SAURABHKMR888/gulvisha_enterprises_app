package com.gulvisha.backend.ai.provider;

public interface AiProvider {
    String getProviderName();
    AiChatResponse chat(AiChatRequest request);

    /**
     * Generates embedding vectors for the given inputs. Providers that do not
     * support embeddings throw {@link UnsupportedOperationException}.
     */
    default AiEmbeddingResponse embed(AiEmbeddingRequest request) {
        throw new UnsupportedOperationException("Provider '" + getProviderName() + "' does not support embeddings");
    }

    /** Default embedding model used when none is explicitly configured. */
    default String defaultEmbeddingModel() {
        return "nomic-embed-text";
    }
}
