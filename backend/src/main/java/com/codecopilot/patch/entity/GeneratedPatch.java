package com.codecopilot.patch.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "generated_patches", indexes = {
        @Index(name = "idx_patch_project", columnList = "project_id")})
@Getter
@Setter
public class GeneratedPatch extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long repositoryId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 2000)
    private String instruction;

    @Column(length = 4000)
    private String summary;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String diff;

    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    private Instant approvedAt;

    private Instant appliedAt;
}