package com.codecopilot.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BugAnalysisResponse {

    private String analysis;
    private String confidence;
    private String likelyRootCause;
    private List<Evidence> evidence;
    private String errorClass;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Evidence {
        private String filePath;
        private String className;
        private String methodName;
        private Integer startLine;
        private Integer endLine;
        private String snippet;
        private String type;
    }
}