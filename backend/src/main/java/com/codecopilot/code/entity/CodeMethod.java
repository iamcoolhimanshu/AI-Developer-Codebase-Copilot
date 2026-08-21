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
@Table(name = "code_methods", indexes = {
        @Index(name = "idx_cm_repo", columnList = "repository_id"),
        @Index(name = "idx_cm_name", columnList = "name"),
        @Index(name = "idx_cm_class", columnList = "class_id")})
@Getter
@Setter
public class CodeMethod extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long repositoryId;

    private Long fileId;

    private Long classId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String returnType;

    @Column(columnDefinition = "TEXT")
    private String parametersJson;

    @Column(columnDefinition = "TEXT")
    private String annotations;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String body;

    private int startLine;

    private int endLine;

    private boolean constructor;

    private boolean isStatic;

    private boolean isPublic;

    @Column(length = 16)
    private String httpMethod;

    @Column(length = 500)
    private String httpPath;
}