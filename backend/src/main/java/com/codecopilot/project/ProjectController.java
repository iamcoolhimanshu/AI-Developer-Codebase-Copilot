package com.codecopilot.project;

import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.project.dto.AddMemberRequest;
import com.codecopilot.project.dto.ProjectDto;
import com.codecopilot.project.dto.ProjectRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDto>> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Project created", projectService.create(request)));
    }

    @GetMapping
    public ApiResponse<List<ProjectDto>> list() {
        return ApiResponse.ok(projectService.listMine());
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDto> get(@PathVariable Long projectId) {
        return ApiResponse.ok(projectService.get(projectId));
    }

    @PatchMapping("/{projectId}")
    public ApiResponse<ProjectDto> update(@PathVariable Long projectId, @RequestBody ProjectRequest request) {
        return ApiResponse.ok(projectService.update(projectId, request));
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId) {
        projectService.delete(projectId);
        return ApiResponse.ok();
    }

    @GetMapping("/{projectId}/dashboard")
    public ApiResponse<Object> dashboard(@PathVariable Long projectId) {
        return ApiResponse.ok(projectService.dashboard(projectId));
    }

    @GetMapping("/{projectId}/members")
    public ApiResponse<List<ProjectDto.ProjectMemberDto>> members(@PathVariable Long projectId) {
        return ApiResponse.ok(projectService.members(projectId));
    }

    @PostMapping("/{projectId}/members")
    public ApiResponse<Void> addMember(@PathVariable Long projectId, @Valid @RequestBody AddMemberRequest request) {
        projectService.addMember(projectId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long projectId, @PathVariable Long userId) {
        projectService.removeMember(projectId, userId);
        return ApiResponse.ok();
    }
}