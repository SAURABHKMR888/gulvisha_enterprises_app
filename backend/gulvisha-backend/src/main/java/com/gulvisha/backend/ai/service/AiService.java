package com.gulvisha.backend.ai.service;

import com.gulvisha.backend.ai.dto.*;
import com.gulvisha.backend.ai.entity.*;
import com.gulvisha.backend.ai.provider.*;
import com.gulvisha.backend.ai.repository.*;
import com.gulvisha.backend.security.UserContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AiService {

    private final AiConfigurationRepository configRepository;
    private final AiPromptRepository promptRepository;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AiProviderFactory providerFactory;

    public AiService(AiConfigurationRepository configRepository,
                    AiPromptRepository promptRepository,
                    AiConversationRepository conversationRepository,
                    AiMessageRepository messageRepository,
                    AiProviderFactory providerFactory) {
        this.configRepository = configRepository;
        this.promptRepository = promptRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.providerFactory = providerFactory;
    }

    public AiConfigDto getConfig() {
        UUID orgId = UserContext.getOrganizationId();
        AiConfiguration config = configRepository.findByOrganizationId(orgId)
                .orElseGet(() -> configRepository.save(new AiConfiguration(orgId)));
        return AiConfigDto.fromEntity(config);
    }

    public AiConfigDto updateConfig(AiConfigDto dto) {
        UUID orgId = UserContext.getOrganizationId();
        AiConfiguration config = configRepository.findByOrganizationId(orgId)
                .orElseGet(() -> new AiConfiguration(orgId));
        if (dto.provider() != null) config.setProvider(dto.provider());
        if (dto.model() != null) config.setModel(dto.model());
        if (dto.baseUrl() != null) config.setBaseUrl(dto.baseUrl());
        config.setEnabled(dto.enabled());
        if (dto.temperature() != null) config.setTemperature(dto.temperature());
        if (dto.maxTokens() != null) config.setMaxTokens(dto.maxTokens());
        return AiConfigDto.fromEntity(configRepository.save(config));
    }

    /**
     * Stores or clears the provider API key for the current organization.
     * A non-blank key overwrites the stored one; null keeps the existing key.
     */
    public AiConfigDto updateApiKey(AiApiKeyRequest request) {
        UUID orgId = UserContext.getOrganizationId();
        AiConfiguration config = configRepository.findByOrganizationId(orgId)
                .orElseGet(() -> configRepository.save(new AiConfiguration(orgId)));
        if (request.apiKey() != null) config.setApiKey(request.apiKey().trim().isEmpty() ? null : request.apiKey().trim());
        return AiConfigDto.fromEntity(configRepository.save(config));
    }

    public List<AiPromptDto> listPrompts() {
        UUID orgId = UserContext.getOrganizationId();
        return promptRepository.findAllByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(AiPromptDto::fromEntity).toList();
    }

    public AiPromptDto createPrompt(AiPromptDto dto) {
        UUID orgId = UserContext.getOrganizationId();
        AiPrompt prompt = new AiPrompt(orgId, dto.name(), dto.content());
        if (dto.description() != null) prompt.setDescription(dto.description());
        if (dto.type() != null) prompt.setType(dto.type());
        return AiPromptDto.fromEntity(promptRepository.save(prompt));
    }

    public AiPromptDto updatePrompt(UUID id, AiPromptDto dto) {
        UUID orgId = UserContext.getOrganizationId();
        AiPrompt prompt = promptRepository.findById(id)
                .filter(p -> p.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found: " + id));
        if (dto.name() != null) prompt.setName(dto.name());
        if (dto.description() != null) prompt.setDescription(dto.description());
        if (dto.content() != null) prompt.setContent(dto.content());
        if (dto.type() != null) prompt.setType(dto.type());
        return AiPromptDto.fromEntity(promptRepository.save(prompt));
    }

    public void deletePrompt(UUID id) {
        UUID orgId = UserContext.getOrganizationId();
        AiPrompt prompt = promptRepository.findById(id)
                .filter(p -> p.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found: " + id));
        promptRepository.delete(prompt);
    }

    public AiChatResponseDto sendMessage(AiChatRequestDto dto) {
        UUID orgId = UserContext.getOrganizationId();
        String userId = UserContext.get().username();

        AiConfiguration config = configRepository.findByOrganizationId(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AI is not configured yet. Open the AI page, go to the Configuration tab and save a configuration."));
        if (!config.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AI is disabled for your organization. Open the AI page, tick 'Enabled' in the Configuration tab and click Save Configuration.");
        }
        if ((config.getProvider().equalsIgnoreCase("gemini") || config.getProvider().equalsIgnoreCase("openai"))
                && (config.getApiKey() == null || config.getApiKey().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No API key stored for provider '" + config.getProvider() + "'. Add it in the AI Configuration tab and save.");
        }

        AiConversation conversation;
        if (dto.conversationId() != null && !dto.conversationId().isBlank()) {
            conversation = conversationRepository.findById(UUID.fromString(dto.conversationId()))
                    .filter(c -> c.getOrganizationId().equals(orgId))
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        } else {
            String title = dto.message().substring(0, Math.min(50, dto.message().length()));
            conversation = conversationRepository.save(new AiConversation(orgId, userId, title));
        }

        AiMessage userMsg = new AiMessage(conversation.getId(), "user", dto.message());
        messageRepository.save(userMsg);

        List<AiChatRequest.ChatMessage> history = new java.util.ArrayList<>(
                messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                        .map(m -> new AiChatRequest.ChatMessage(m.getRole(), m.getContent())).toList());

        if (dto.promptId() != null && !dto.promptId().isBlank()) {
            AiPrompt systemPrompt = promptRepository.findById(UUID.fromString(dto.promptId()))
                    .filter(p -> p.getOrganizationId().equals(orgId)).orElse(null);
            if (systemPrompt != null) {
                history.add(0, new AiChatRequest.ChatMessage("system", systemPrompt.getContent()));
            }
        }

        AiProvider provider = providerFactory.getProvider(config.getProvider());
        AiChatRequest request = new AiChatRequest(
                config.getModel(), history, config.getTemperature(), config.getMaxTokens(),
                config.getApiKey(), config.getBaseUrl());
        AiChatResponse response = provider.chat(request);

        AiMessage assistantMsg = new AiMessage(conversation.getId(), "assistant", response.content());
        assistantMsg.setTokens(response.tokensUsed());
        messageRepository.save(assistantMsg);

        return new AiChatResponseDto(
                conversation.getId().toString(), "assistant",
                response.content(), response.tokensUsed(), Instant.now().toString());
    }

    public List<AiConversationDto> listConversations() {
        UUID orgId = UserContext.getOrganizationId();
        String userId = UserContext.get().username();
        return conversationRepository.findAllByOrganizationIdAndUserIdOrderByUpdatedAtDesc(orgId, userId).stream()
                .map(c -> AiConversationDto.fromEntity(c, List.of())).toList();
    }

    public AiConversationDto getConversation(UUID id) {
        UUID orgId = UserContext.getOrganizationId();
        AiConversation conversation = conversationRepository.findById(id)
                .filter(c -> c.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        List<AiMessage> messages = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(id);
        return AiConversationDto.fromEntity(conversation, messages);
    }

    public void deleteConversation(UUID id) {
        UUID orgId = UserContext.getOrganizationId();
        AiConversation conversation = conversationRepository.findById(id)
                .filter(c -> c.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        messageRepository.findAllByConversationIdOrderByCreatedAtAsc(id).forEach(messageRepository::delete);
        conversationRepository.delete(conversation);
    }

    public List<String> availableProviders() {
        return providerFactory.availableProviders();
    }
}
