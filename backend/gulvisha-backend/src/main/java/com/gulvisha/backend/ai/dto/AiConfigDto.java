package com.gulvisha.backend.ai.dto;

import com.gulvisha.backend.ai.entity.AiConfiguration;

/** * DTO for AI configuration — never exposes the API key directly.
 */
public record AiConfigDto(
    String id,
    String organizationId,
    String provider,
    String model,
    String baseUrl,
    boolean enabled,
    Double temperature,
    Integer maxTokens,
    boolean hasApiKey
) {
    public static AiConfigDto fromEntity(AiConfiguration entity) {
        return new AiConfigDto(
            entity.getId().toString(),
            entity.getOrganizationId().toString(),
            entity.getProvider(),
            entity.getModel(),
            entity.getBaseUrl(),
            entity.isEnabled(),
            entity.getTemperature(),
            entity.getMaxTokens(),
            entity.getApiKey() != null && !entity.getApiKey().isBlank()
        );
    }
}
