package com.codecopilot.ai;

import org.springframework.ai.chat.client.ChatClient;

public interface AiService {

	String chat(String systemPrompt, String userPrompt);

	String chat(String systemPrompt, String userPrompt, ChatClient client);

	reactor.core.publisher.Flux<String> stream(String systemPrompt, String userPrompt);

	boolean isConfigured();
}