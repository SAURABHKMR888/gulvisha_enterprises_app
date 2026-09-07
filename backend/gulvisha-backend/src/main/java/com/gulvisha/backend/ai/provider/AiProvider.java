package com.gulvisha.backend.ai.provider;

public interface AiProvider {
    String getProviderName();
    AiChatResponse chat(AiChatRequest request);
}
