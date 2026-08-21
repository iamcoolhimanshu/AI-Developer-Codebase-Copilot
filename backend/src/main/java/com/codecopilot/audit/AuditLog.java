package com.codecopilot.audit;

import com.codecopilot.common.entity.BaseEntity;
import com.codecopilot.common.security.SecurityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_project", columnList = "project_id")})
@Getter
@Setter
public class AuditLog extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    private Long projectId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(length = 2000)
    private String entityRef;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(length = 64)
    private String ip;
}