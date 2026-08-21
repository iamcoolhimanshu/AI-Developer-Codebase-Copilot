package com.codecopilot.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDashboardDto {

    private Long projectId;
    private String name;
    private List<String> technologies;
    private List<RepositorySummary> repositories;
    private Map<String, Long> counts;
    private String analysisStatus;
    private Instant lastIndexedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepositorySummary {
        private Long id;
        private String name;
        private String url;
        private String branch;
        private String provider;
        private String status;
        private Instant lastIndexedAt;
        private long fileCount;
    }
}