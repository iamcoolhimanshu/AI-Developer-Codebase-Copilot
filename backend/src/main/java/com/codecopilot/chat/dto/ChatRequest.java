package com.codecopilot.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank
    private String message;

    private Long conversationId;

    private java.util.List<Long> repositoryIds;
}