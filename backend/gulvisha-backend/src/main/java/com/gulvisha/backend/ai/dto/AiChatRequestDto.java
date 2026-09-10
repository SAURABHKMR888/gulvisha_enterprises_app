package com.gulvisha.backend.ai.dto;

/**
 * Chat request. knowledgeBaseMode controls RAG grounding:
 * - "strict": answer only from the knowledge base (refuses when nothing relevant is found)
 * - "general": skip the knowledge base entirely
 * - null/"auto" (default): inject knowledge-base context when retrieval finds relevant chunks
 */
public record AiChatRequestDto(
    String conversationId,
    String message,
    String promptId,
    String knowledgeBaseMode
) {}