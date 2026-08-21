package com.codecopilot.ai.tools.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tool_executions", indexes = {
        @Index(name = "idx_te_project", columnList = "project_id")})
@Getter
@Setter
public class ToolExecution extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String toolName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String inputJson;

    @Column(length = 2000)
    private String resultSummary;

    private boolean readOnly;

    private boolean authorized;

    private long durationMs;
}