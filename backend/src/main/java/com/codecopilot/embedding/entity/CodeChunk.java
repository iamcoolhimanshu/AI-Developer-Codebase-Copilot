package com.codecopilot.embedding.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "code_chunks", indexes = {
        @Index(name = "idx_ch_repo", columnList = "repository_id"),
        @Index(name = "idx_ch_type", columnList = "chunk_type")})
@Getter
@Setter
public class CodeChunk extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long repositoryId;

    private Long fileId;

    @Column(nullable = false, length = 32)
    private String chunkType;

    private Long classId;

    @Column(length = 255)
    private String className;

    private Long methodId;

    @Column(length = 255)
    private String methodName;

    @Column(length = 2000)
    private String filePath;

    private int startLine;

    private int endLine;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 64)
    private String checksum;
}