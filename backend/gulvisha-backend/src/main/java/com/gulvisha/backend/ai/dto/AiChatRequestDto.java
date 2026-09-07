package com.gulvisha.backend.ai.dto;

public record AiChatRequestDto(
    String conversationId,
    String message,
    String promptId
) {}
