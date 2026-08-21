package com.codecopilot.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexStatusDto {

    private Long repositoryId;
    private Long indexJobId;
    private String status;
    private String phase;
    private int progress;
    private String error;
    private boolean incremental;
    private Instant startedAt;
    private Instant finishedAt;
    private long fileCount;
    private long classCount;
    private long methodCount;
    private long chunkCount;
}