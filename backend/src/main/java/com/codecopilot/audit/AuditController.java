package com.codecopilot.audit;

import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.project.ProjectAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/audit")
public class AuditController {

    private final AuditLogRepository repository;
    private final ProjectAccessService accessService;

    public AuditController(AuditLogRepository repository, ProjectAccessService accessService) {
        this.repository = repository;
        this.accessService = accessService;
    }

    @GetMapping
    public ApiResponse<List<AuditLog>> logs(@PathVariable Long projectId) {
        accessService.requireView(projectId);
        return ApiResponse.ok(repository.findByProjectIdOrderByCreatedAtDesc(projectId));
    }
}