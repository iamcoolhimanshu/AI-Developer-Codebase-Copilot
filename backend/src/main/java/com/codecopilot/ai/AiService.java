package com.codecopilot.ai;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Provider-isolated AI abstraction. Grok is the initial provider (via the
 * OpenAI-compatible endpoint); Spring AI handles the integration so other
 * providers can be added later without touching business logic.
 */
public interface AiService {

    String chat(String systemPrompt, String userPrompt);

    String chat(String systemPrompt, String userPrompt, ChatClient client);

    reactor.core.publisher.Flux<String> stream(String systemPrompt, String userPrompt);

    boolean isConfigured();
}