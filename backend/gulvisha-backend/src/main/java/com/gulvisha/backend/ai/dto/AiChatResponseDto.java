package com.gulvisha.backend.ai.dto;

import java.util.List;

public record AiChatResponseDto(
    String conversationId,
    String role,
    String content,
    Integer tokens,
    String timestamp,
    List<String> sources
) {}