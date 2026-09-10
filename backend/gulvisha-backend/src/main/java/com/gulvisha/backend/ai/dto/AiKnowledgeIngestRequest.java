package com.gulvisha.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to ingest a knowledge document: the raw text is chunked and embedded.
 */
public record AiKnowledgeIngestRequest(
    @NotBlank String title,
    @NotBlank String content
) {}
