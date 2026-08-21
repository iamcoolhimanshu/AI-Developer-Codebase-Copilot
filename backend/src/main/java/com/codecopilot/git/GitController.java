package com.codecopilot.git;

import com.codecopilot.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/git")
public class GitController {

    private final GitIntelligenceService gitService;

    public GitController(GitIntelligenceService gitService) {
        this.gitService = gitService;
    }

    @GetMapping("/repositories/{repositoryId}/commits")
    public ApiResponse<List<GitIntelligenceService.CommitInfo>> commits(@PathVariable Long projectId,
                                                                        @PathVariable Long repositoryId,
                                                                        @RequestParam(required = false) String file,
                                                                        @RequestParam(defaultValue = "50") int max) {
        return ApiResponse.ok(gitService.commits(projectId, repositoryId, file, max));
    }

    @GetMapping("/repositories/{repositoryId}/commits/{commitId}/diff")
    public ApiResponse<GitIntelligenceService.DiffResult> diff(@PathVariable Long projectId,
                                                               @PathVariable Long repositoryId,
                                                               @PathVariable String commitId) {
        return ApiResponse.ok(gitService.diff(projectId, repositoryId, commitId));
    }

    @GetMapping("/repositories/{repositoryId}/blame")
    public ApiResponse<List<GitIntelligenceService.BlameLine>> blame(@PathVariable Long projectId,
                                                                     @PathVariable Long repositoryId,
                                                                     @RequestParam String file) {
        return ApiResponse.ok(gitService.blame(projectId, repositoryId, file));
    }
}