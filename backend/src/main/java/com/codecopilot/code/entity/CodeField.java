package com.codecopilot.code.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "code_fields", indexes = {
        @Index(name = "idx_cf_repo", columnList = "repository_id"),
        @Index(name = "idx_cf_class", columnList = "class_id")})
@Getter
@Setter
public class CodeField extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long repositoryId;

    private Long fileId;

    private Long classId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String modifiers;

    @Column(columnDefinition = "TEXT")
    private String annotations;

    private int startLine;

    private int endLine;
}