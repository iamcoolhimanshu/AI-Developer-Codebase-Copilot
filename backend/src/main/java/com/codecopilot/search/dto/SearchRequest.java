package com.codecopilot.search.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SearchRequest {

    @NotBlank
    private String query;

    private Integer topK = 20;

    private List<Long> repositoryIds;

    private String language;

    private List<String> types;
}