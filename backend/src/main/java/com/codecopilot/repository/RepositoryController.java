package com.codecopilot.repository;

import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.indexing.IndexJob;
import com.codecopilot.indexing.IndexJobRepository;
import com.codecopilot.indexing.IndexService;
import com.codecopilot.repository.dto.ConnectRequest;
import com.codecopilot.repository.dto.IndexStatusDto;
import com.codecopilot.repository.dto.RepositoryDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final IndexService indexService;
    private final IndexJobRepository jobRepository;

    public RepositoryController(RepositoryService repositoryService, IndexService indexService,
                                IndexJobRepository jobRepository) {
        this.repositoryService = repositoryService;
        this.indexService = indexService;
        this.jobRepository = jobRepository;
    }

    @GetMapping("/projects/{projectId}/repositories")
    public ApiResponse<List<RepositoryDto>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(repositoryService.list(projectId));
    }

    @PostMapping("/projects/{projectId}/repositories")
    public ResponseEntity<ApiResponse<RepositoryDto>> connect(
            @PathVariable Long projectId,
            @Valid @RequestBody ConnectRequest request,
            @RequestHeader(name = "X-GitHub-Token", required = false) String token) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Repository connected", repositoryService.connect(projectId, request, token)));
    }

    @PostMapping("/projects/{projectId}/repositories/upload")
    public ResponseEntity<ApiResponse<RepositoryDto>> upload(
            @PathVariable Long projectId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String name) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Repository uploaded", repositoryService.upload(projectId, file, name)));
    }

    @DeleteMapping("/repositories/{repositoryId}")
    public ApiResponse<Void> delete(@PathVariable Long repositoryId, @RequestParam Long projectId) {
        repositoryService.delete(repositoryId, projectId);
        return ApiResponse.ok();
    }

    @PostMapping("/repositories/{repositoryId}/index")
    public ApiResponse<IndexStatusDto> startIndex(@PathVariable Long repositoryId, @RequestParam Long projectId) {
        GitRepository repo = repositoryService.requireRepository(repositoryId, projectId);
        IndexJob job = indexService.createJob(repo.getId());
        indexService.indexAsync(job.getId());
        return ApiResponse.ok(toStatus(job));
    }

    @GetMapping("/repositories/{repositoryId}/index/status")
    public ApiResponse<IndexStatusDto> indexStatus(@PathVariable Long repositoryId, @RequestParam Long projectId) {
        repositoryService.requireRepository(repositoryId, projectId);
        IndexJob latest = jobRepository.findTopByRepositoryIdOrderByCreatedAtDesc(repositoryId)
                .orElse(null);
        return ApiResponse.ok(latest == null ? null : toStatus(latest));
    }

    private IndexStatusDto toStatus(IndexJob job) {
        return IndexStatusDto.builder()
                .repositoryId(job.getRepositoryId())
                .indexJobId(job.getId())
                .status(job.getStatus().name())
                .phase(job.getPhase())
                .progress(job.getProgress())
                .error(job.getError())
                .incremental(job.isIncremental())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .fileCount(job.getFileCount())
                .classCount(job.getClassCount())
                .methodCount(job.getMethodCount())
                .chunkCount(job.getChunkCount())
                .build();
    }
}