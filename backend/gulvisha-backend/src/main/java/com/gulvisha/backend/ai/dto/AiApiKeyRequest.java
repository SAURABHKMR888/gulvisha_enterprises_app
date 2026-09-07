package com.gulvisha.backend.ai.dto;

/**
 * Request body for saving the AI provider API key.
 * The key itself is never returned to clients — only hasApiKey on AiConfigDto.
 */
public record AiApiKeyRequest(String apiKey) {
}
