package com.gulvisha.backend.ai.dto;

import com.gulvisha.backend.ai.entity.AiKnowledgeDocument;

import java.util.UUID;

/**
 * Knowledge document view. The raw content is excluded from list payloads
 * (only a short preview is included) to keep responses light.
 */
public record AiKnowledgeDocumentDto(
    UUID id,
    String title,
    String status,
    int chunkCount,
    String error,
    String preview,
    String createdAt,
    String updatedAt
) {
    public static AiKnowledgeDocumentDto fromEntity(AiKnowledgeDocument doc) {
        String content = doc.getContent();
        String preview = content != null && content.length() > 200
                ? content.substring(0, 200) + "…" : content;
        return new AiKnowledgeDocumentDto(
                doc.getId(),
                doc.getTitle(),
                doc.getStatus(),
                doc.getChunkCount(),
                doc.getError(),
                preview,
                doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null,
                doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : null);
    }
}
