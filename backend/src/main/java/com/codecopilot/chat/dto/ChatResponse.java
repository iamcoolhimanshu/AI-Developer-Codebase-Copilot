package com.codecopilot.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private Long conversationId;
    private Long messageId;
    private String content;
    private List<Source> sources;
    private List<String> toolsUsed;
    private String model;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private String filePath;
        private String className;
        private String methodName;
        private int startLine;
        private int endLine;
        private String snippet;
        private String sourceType;
        private double score;
    }
}