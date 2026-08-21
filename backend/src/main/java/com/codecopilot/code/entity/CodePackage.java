package com.codecopilot.code.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "code_packages", indexes = {
        @Index(name = "idx_cp_repo", columnList = "repository_id")})
@Getter
@Setter
public class CodePackage extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long repositoryId;

    private Long fileId;

    @Column(nullable = false, length = 500)
    private String name;
}