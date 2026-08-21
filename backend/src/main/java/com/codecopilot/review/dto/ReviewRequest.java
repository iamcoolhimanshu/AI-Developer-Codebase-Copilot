package com.codecopilot.review.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ReviewRequest {

    @NotBlank
    private String targetType;

    private Long repositoryId;

    private String filePath;

    private String commitId;

    private String diff;

    private List<Long> repositoryIds;

    public enum TargetType {
        FILE, COMMIT, BRANCH, PR, DIFF
    }
}