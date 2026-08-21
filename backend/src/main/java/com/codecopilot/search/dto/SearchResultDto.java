package com.codecopilot.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {

    private String matchType;
    private String filePath;
    private String className;
    private String methodName;
    private String kind;
    private int startLine;
    private int endLine;
    private String snippet;
    private String sourceType;
    private double score;
    private Long repositoryId;
}