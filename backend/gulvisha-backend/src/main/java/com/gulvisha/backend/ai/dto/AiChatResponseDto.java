package com.gulvisha.backend.ai.dto;

public record AiChatResponseDto(
    String conversationId,
    String role,
    String content,
    Integer tokens,
    String timestamp
) {}
