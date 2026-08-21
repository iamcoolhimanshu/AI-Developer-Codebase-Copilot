package com.codecopilot.code.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "code_classes", indexes = {
        @Index(name = "idx_cc_repo", columnList = "repository_id"),
        @Index(name = "idx_cc_name", columnList = "name"),
        @Index(name = "idx_cc_fq", columnList = "fq_name")})
@Getter
@Setter
public class CodeClass extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long repositoryId;

    private Long fileId;

    private Long packageId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 700, name = "fq_name")
    private String fqName;

    @Column(nullable = false, length = 16)
    private String kind;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String annotations;

    @Column(columnDefinition = "TEXT")
    private String modifiers;

    @Column(length = 500)
    private String parentClass;

    @Column(columnDefinition = "TEXT")
    private String interfaces;

    private int startLine;

    private int endLine;
}