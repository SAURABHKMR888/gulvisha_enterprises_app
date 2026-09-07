package com.gulvisha.backend.ai.dto;

import com.gulvisha.backend.ai.entity.AiConversation;
import com.gulvisha.backend.ai.entity.AiMessage;

import java.util.List;

/** * DTO for a conversation with its messages.
 */
public record AiConversationDto(
    String id,
    String title,
    String createdAt,
    String updatedAt,
    List<AiMessageDto> messages
) {
    public static AiConversationDto fromEntity(AiConversation conv, List<AiMessage> messages) {
        return new AiConversationDto(
            conv.getId().toString(),
            conv.getTitle(),
            conv.getCreatedAt().toString(),
            conv.getUpdatedAt().toString(),
            messages.stream().map(AiMessageDto::fromEntity).toList()
        );
    }
}

/** * DTO for a single message.
 */
record AiMessageDto(
    String id,
    String role,
    String content,
    Integer tokens,
    String createdAt
) {
    static AiMessageDto fromEntity(AiMessage entity) {
        return new AiMessageDto(
            entity.getId().toString(),
            entity.getRole(),
            entity.getContent(),
            entity.getTokens(),
            entity.getCreatedAt().toString()
        );
    }
}
