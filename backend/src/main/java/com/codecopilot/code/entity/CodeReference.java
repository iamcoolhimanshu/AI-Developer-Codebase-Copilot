package com.codecopilot.code.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "code_references", indexes = {
        @Index(name = "idx_cr_repo", columnList = "repository_id"),
        @Index(name = "idx_cr_target", columnList = "target_name")})
@Getter
@Setter
public class CodeReference extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long repositoryId;

    private Long fileId;

    private Long sourceClassId;

    private Long sourceMethodId;

    @Column(nullable = false, length = 700)
    private String sourceClassFq;

    @Column(length = 255)
    private String sourceMethodName;

    @Column(nullable = false, length = 255)
    private String targetName;

    @Column(length = 700)
    private String targetClassFq;

    @Column(nullable = false, length = 32)
    private String type;

    private int lineNumber;
}