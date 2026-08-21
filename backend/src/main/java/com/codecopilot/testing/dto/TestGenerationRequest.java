package com.codecopilot.testing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TestGenerationRequest {

    @NotNull
    private Long classId;

    private String methodName;
}