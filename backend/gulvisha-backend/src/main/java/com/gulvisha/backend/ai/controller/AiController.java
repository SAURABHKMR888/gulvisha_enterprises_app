package com.gulvisha.backend.ai.controller;

import com.gulvisha.backend.ai.dto.*;
import com.gulvisha.backend.ai.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/providers")
    public List<String> getProviders() {
        return aiService.availableProviders();
    }

    @GetMapping("/config")
    public AiConfigDto getConfig() {
        return aiService.getConfig();
    }

    @PutMapping("/config")
    public AiConfigDto updateConfig(@RequestBody AiConfigDto dto) {
        return aiService.updateConfig(dto);
    }

    @PutMapping("/config/api-key")
    public AiConfigDto updateApiKey(@RequestBody AiApiKeyRequest request) {
        return aiService.updateApiKey(request);
    }

    @GetMapping("/prompts")
    public List<AiPromptDto> listPrompts() {
        return aiService.listPrompts();
    }

    @PostMapping("/prompts")
    public AiPromptDto createPrompt(@RequestBody AiPromptDto dto) {
        return aiService.createPrompt(dto);
    }

    @PutMapping("/prompts/{id}")
    public AiPromptDto updatePrompt(@PathVariable UUID id, @RequestBody AiPromptDto dto) {
        return aiService.updatePrompt(id, dto);
    }

    @DeleteMapping("/prompts/{id}")
    public ResponseEntity<Void> deletePrompt(@PathVariable UUID id) {
        aiService.deletePrompt(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/chat")
    public AiChatResponseDto chat(@RequestBody AiChatRequestDto dto) {
        return aiService.sendMessage(dto);
    }

    @GetMapping("/conversations")
    public List<AiConversationDto> listConversations() {
        return aiService.listConversations();
    }

    @GetMapping("/conversations/{id}")
    public AiConversationDto getConversation(@PathVariable UUID id) {
        return aiService.getConversation(id);
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable UUID id) {
        aiService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
