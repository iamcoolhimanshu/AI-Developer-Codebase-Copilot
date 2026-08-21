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
@Table(name = "code_embeddings", indexes = {
        @Index(name = "idx_ce_repo", columnList = "repository_id"),
        @Index(name = "idx_ce_chunk", columnList = "chunk_id")})
@Getter
@Setter
public class CodeEmbedding extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long repositoryId;

    private Long fileId;

    @Column(nullable = false)
    private Long chunkId;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String vectorJson;

    private Integer dimension;

    @Column(length = 64)
    private String model;
}