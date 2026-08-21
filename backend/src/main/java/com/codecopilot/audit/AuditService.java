package com.codecopilot.audit;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(Long projectId, String action, String entityRef, Object detail) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(com.codecopilot.common.security.SecurityUtils.currentUserId());
            log.setProjectId(projectId);
            log.setAction(action);
            log.setEntityRef(entityRef);
            log.setDetail(detail == null ? null : String.valueOf(detail));
            repository.save(log);
        } catch (Exception ignored) {
            // audit must never break the primary operation
        }
    }

    public void logGlobal(String action, String entityRef, Object detail) {
        log(null, action, entityRef, detail);
    }
}