package com.gulvisha.backend.ai.provider;

/** * Chat response returned from any AI provider.
 */
public record AiChatResponse(
    String content,
    String model,
    Integer tokensUsed
) {}
