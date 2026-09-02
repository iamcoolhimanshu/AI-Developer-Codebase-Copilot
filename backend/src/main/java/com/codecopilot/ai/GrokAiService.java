package com.codecopilot.ai;

import com.codecopilot.common.exception.BadRequestException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;

@Service
public class GrokAiService implements AiService {

    private final ChatClient.Builder chatClientBuilder;
    private final boolean configured;

    public GrokAiService(ChatClient.Builder chatClientBuilder,
                         @Value("${app.ai.api-key:}") String apiKey) {
        this.chatClientBuilder = chatClientBuilder;
        this.configured = apiKey != null && !apiKey.isBlank();
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, null);
    }

    @Override
    public String chat(String systemPrompt, String userPrompt, ChatClient client) {
        if (!configured) {
            throw new BadRequestException(
                    "AI is not configured. Set OPENAI_API_KEY (or XAI_API_KEY) and OPENAI_MODEL (or XAI_MODEL) in the environment first. "
                            + "For OpenAI: OPENAI_API_KEY=sk-... OPENAI_MODEL=gpt-4o-mini OPENAI_BASE_URL=https://api.openai.com/v1. "
                            + "For Grok: XAI_API_KEY=xai-... XAI_MODEL=grok-4 XAI_BASE_URL=https://api.x.ai/v1.");
        }
        ChatClient chatClient = client != null ? client : chatClientBuilder.build();
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public Flux<String> stream(String systemPrompt, String userPrompt) {
        if (!configured) {
            return Flux.just(
                    "AI is not configured. Set OPENAI_API_KEY (or XAI_API_KEY) and OPENAI_MODEL (or XAI_MODEL) in the environment first.");
        }
        return chatClientBuilder.build().prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content();
    }
}