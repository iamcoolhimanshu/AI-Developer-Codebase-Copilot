package com.codecopilot.code.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "code_dependencies", indexes = {
        @Index(name = "idx_cd_repo", columnList = "repository_id"),
        @Index(name = "idx_cd_src", columnList = "source_class_fq"),
        @Index(name = "idx_cd_tgt", columnList = "target_class_fq")})
@Getter
@Setter
public class CodeDependency extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long repositoryId;

    private Long fileId;

    private Long sourceClassId;

    @Column(nullable = false, length = 700)
    private String sourceClassFq;

    @Column(nullable = false, length = 700)
    private String targetClassFq;

    @Column(nullable = false, length = 32)
    private String type;

    private int lineNumber;
}