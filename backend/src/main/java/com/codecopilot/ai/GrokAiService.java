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
                    "AI is not configured. Set XAI_API_KEY (and XAI_MODEL) in the environment first.");
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
            return Flux.just("AI is not configured. Set XAI_API_KEY (and XAI_MODEL) in the environment first.");
        }
        return chatClientBuilder.build().prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content();
    }
}