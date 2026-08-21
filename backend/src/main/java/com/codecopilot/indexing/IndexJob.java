package com.codecopilot.indexing;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "index_jobs", indexes = {
        @Index(name = "idx_ij_repo", columnList = "repository_id")})
@Getter
@Setter
public class IndexJob extends BaseEntity {

    public enum IndexStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }

    @Column(nullable = false)
    private Long repositoryId;

    @Column(nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IndexStatus status = IndexStatus.PENDING;

    @Column(length = 64)
    private String phase;

    private int progress;

    @Column(length = 4000)
    private String error;

    private boolean incremental;

    private Instant startedAt;

    private Instant finishedAt;

    private long fileCount;

    private long classCount;

    private long methodCount;

    private long chunkCount;
}