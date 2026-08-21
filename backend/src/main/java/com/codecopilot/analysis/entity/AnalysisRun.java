package com.codecopilot.analysis.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "analysis_runs", indexes = {
        @Index(name = "idx_ar_project", columnList = "project_id")})
@Getter
@Setter
public class AnalysisRun extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 16)
    private String status;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String input;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String result;
}