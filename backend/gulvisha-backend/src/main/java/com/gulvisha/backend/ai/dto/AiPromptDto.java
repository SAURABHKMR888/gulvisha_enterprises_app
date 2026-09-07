package com.gulvisha.backend.ai.dto;

import com.gulvisha.backend.ai.entity.AiPrompt;

/** * DTO for prompt templates.
 */
public record AiPromptDto(
    String id,
    String name,
    String description,
    String content,
    String type,
    String createdAt
) {
    public static AiPromptDto fromEntity(AiPrompt entity) {
        return new AiPromptDto(
            entity.getId().toString(),
            entity.getName(),
            entity.getDescription(),
            entity.getContent(),
            entity.getType(),
            entity.getCreatedAt().toString()
        );
    }
}
