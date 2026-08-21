package com.codecopilot.analysis.entity;

import com.codecopilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "analysis_findings", indexes = {
        @Index(name = "idx_af_run", columnList = "run_id")})
@Getter
@Setter
public class AnalysisFinding extends BaseEntity {

    @Column(nullable = false)
    private Long runId;

    @Column(nullable = false)
    private Long projectId;

    @Column(length = 64)
    private String type;

    @Column(length = 2000)
    private String filePath;

    @Column(length = 2000)
    private String detail;
}