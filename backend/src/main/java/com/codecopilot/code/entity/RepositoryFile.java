package com.codecopilot.code.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "repository_files", indexes = {
        @Index(name = "idx_rf_repo", columnList = "repository_id"),
        @Index(name = "idx_rf_project", columnList = "project_id")})
@Getter
@Setter
public class RepositoryFile extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long repositoryId;

    @Column(nullable = false, length = 2000)
    private String path;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 32)
    private String language;

    private long sizeBytes;

    private int lineCount;

    @Column(length = 64)
    private String checksum;

    @Column(columnDefinition = "TEXT")
    private String content;
}