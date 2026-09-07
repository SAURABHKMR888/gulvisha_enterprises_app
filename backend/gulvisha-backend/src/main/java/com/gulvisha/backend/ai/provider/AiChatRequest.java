package com.gulvisha.backend.ai.provider;

import java.util.List;

public record AiChatRequest(
    String model,
    List<ChatMessage> messages,
    Double temperature,
    Integer maxTokens,
    String apiKey,
    String baseUrl
) {
    public record ChatMessage(String role, String content) {}

    public static AiChatRequest of(String model, String userMessage) {
        return new AiChatRequest(model, List.of(new ChatMessage("user", userMessage)), null, null, null, null);
    }

    public static AiChatRequest withSystem(String model, String systemMessage, String userMessage) {
        return new AiChatRequest(model, List.of(
            new ChatMessage("system", systemMessage),
            new ChatMessage("user", userMessage)
        ), null, null, null, null);
    }
}
