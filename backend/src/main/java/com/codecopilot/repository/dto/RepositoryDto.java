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
public class RepositoryDto {

    private Long id;
    private Long projectId;
    private String name;
    private String url;
    private String branch;
    private String provider;
    private String status;
    private long indexedFileCount;
    private Instant lastIndexedAt;
    private Instant createdAt;
}