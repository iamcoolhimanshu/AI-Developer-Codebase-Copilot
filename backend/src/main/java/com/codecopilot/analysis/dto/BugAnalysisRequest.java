package com.codecopilot.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class BugAnalysisRequest {

    @NotBlank
    private String errorMessage;

    private String stackTrace;

    private String filePath;

    private Integer lineNumber;

    private List<Long> repositoryIds;
}