package com.codecopilot.ai.tools;

import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.project.ProjectAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tools")
public class ToolController {

    private final ToolExecutionRepository executionRepository;
    private final ProjectAccessService accessService;

    public ToolController(ToolExecutionRepository executionRepository, ProjectAccessService accessService) {
        this.executionRepository = executionRepository;
        this.accessService = accessService;
    }

    @GetMapping("/executions")
    public ApiResponse<List<com.codecopilot.ai.tools.entity.ToolExecution>> executions(@PathVariable Long projectId) {
        accessService.requireView(projectId);
        return ApiResponse.ok(executionRepository.findByProjectIdOrderByCreatedAtDesc(projectId));
    }

    @GetMapping("/definitions")
    public ApiResponse<List<ToolDefinitionDto>> definitions() {
        return ApiResponse.ok(List.of(
                new ToolDefinitionDto("searchCode", "Search classes/methods by name", true),
                new ToolDefinitionDto("getFile", "Read a source file", true),
                new ToolDefinitionDto("findClass", "Find a class and its methods", true),
                new ToolDefinitionDto("findMethod", "Find a method with body", true),
                new ToolDefinitionDto("findReferences", "Find usages of a symbol", true),
                new ToolDefinitionDto("findApiEndpoint", "Find REST endpoints", true),
                new ToolDefinitionDto("getGitHistory", "Recent commits", true),
                new ToolDefinitionDto("findTests", "Find related tests", true)));
    }

    public record ToolDefinitionDto(String name, String description, boolean readOnly) {
    }
}