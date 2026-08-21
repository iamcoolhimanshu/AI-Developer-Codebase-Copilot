package com.codecopilot.chat;

import com.codecopilot.chat.dto.ChatRequest;
import com.codecopilot.chat.dto.ChatResponse;
import com.codecopilot.chat.entity.Conversation;
import com.codecopilot.chat.entity.ConversationMessage;
import com.codecopilot.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/chat")
public class ChatController {

    private final ConversationService conversationService;

    public ChatController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<Conversation>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(conversationService.list(projectId));
    }

    @PostMapping("/conversations")
    public ApiResponse<Conversation> create(@PathVariable Long projectId, @RequestParam(required = false) String title) {
        return ApiResponse.ok(conversationService.create(projectId, title));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<ConversationMessage>> messages(@PathVariable Long projectId,
                                                           @PathVariable Long conversationId) {
        return ApiResponse.ok(conversationService.messages(projectId, conversationId));
    }

    @PostMapping("/messages")
    public ApiResponse<ChatResponse> send(@PathVariable Long projectId, @Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(conversationService.send(projectId, request));
    }

    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long projectId, @RequestBody ChatRequest request) {
        return conversationService.stream(projectId, request);
    }
}