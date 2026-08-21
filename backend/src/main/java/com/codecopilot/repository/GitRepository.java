package com.codecopilot.repository;

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
@Table(name = "repositories", indexes = {
        @Index(name = "idx_rep_project", columnList = "project_id")})
@Getter
@Setter
public class GitRepository extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String url;

    @Column(length = 200)
    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RepositoryProvider provider = RepositoryProvider.GITHUB;

    @Column(length = 1000)
    private String localPath;

    private Instant lastIndexedAt;

    @Column(nullable = false)
    private long indexedFileCount;

    @Column(nullable = false, length = 32)
    private String status = "READY";
}