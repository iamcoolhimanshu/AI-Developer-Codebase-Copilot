package com.codecopilot.project;

import com.codecopilot.code.repo.CodeClassRepository;
import com.codecopilot.code.repo.CodeDependencyRepository;
import com.codecopilot.code.repo.CodeMethodRepository;
import com.codecopilot.code.repo.RepositoryFileRepository;
import com.codecopilot.indexing.IndexJob;
import com.codecopilot.indexing.IndexJobRepository;
import com.codecopilot.repository.GitRepository;
import com.codecopilot.repository.GitRepositoryRepository;
import com.codecopilot.project.dto.ProjectDashboardDto;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectDashboardService {

    private final GitRepositoryRepository repositoryRepository;
    private final RepositoryFileRepository fileRepository;
    private final CodeClassRepository classRepository;
    private final CodeMethodRepository methodRepository;
    private final CodeDependencyRepository dependencyRepository;
    private final IndexJobRepository jobRepository;

    public ProjectDashboardService(GitRepositoryRepository repositoryRepository,
                                   RepositoryFileRepository fileRepository,
                                   CodeClassRepository classRepository,
                                   CodeMethodRepository methodRepository,
                                   CodeDependencyRepository dependencyRepository,
                                   IndexJobRepository jobRepository) {
        this.repositoryRepository = repositoryRepository;
        this.fileRepository = fileRepository;
        this.classRepository = classRepository;
        this.methodRepository = methodRepository;
        this.dependencyRepository = dependencyRepository;
        this.jobRepository = jobRepository;
    }

    public ProjectDashboardDto build(Long projectId) {
        List<GitRepository> repos = repositoryRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

        Map<String, Long> counts = new HashMap<>();
        counts.put("files", fileRepository.countByProjectId(projectId));
        counts.put("classes", classRepository.countByProjectId(projectId));
        counts.put("methods", methodRepository.countByProjectId(projectId));
        counts.put("dependencies", dependencyRepository.countByProjectId(projectId));
        counts.put("endpoints", methodRepository.countByProjectIdAndHttpPathNotNull(projectId));

        String status = "NOT_INDEXED";
        java.time.Instant lastIndexed = null;
        for (GitRepository repo : repos) {
            IndexJob latest = jobRepository.findTopByRepositoryIdOrderByCreatedAtDesc(repo.getId()).orElse(null);
            if (latest != null) {
                if (status.equals("NOT_INDEXED") || latest.getStatus() != IndexJob.IndexStatus.COMPLETED) {
                    status = latest.getStatus().name();
                }
            }
        }

        return ProjectDashboardDto.builder()
                .projectId(projectId)
                .name(null)
                .technologies(repos.stream().map(GitRepository::getName).toList())
                .repositories(repos.stream().map(r -> {
                    long fileCount = fileRepository.countByProjectIdAndRepositoryId(projectId, r.getId());
                    IndexJob latest = jobRepository.findTopByRepositoryIdOrderByCreatedAtDesc(r.getId()).orElse(null);
                    return ProjectDashboardDto.RepositorySummary.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .url(r.getUrl())
                            .branch(r.getBranch())
                            .provider(r.getProvider().name())
                            .status(latest == null ? "PENDING" : latest.getStatus().name())
                            .lastIndexedAt(r.getLastIndexedAt())
                            .fileCount(fileCount)
                            .build();
                }).toList())
                .counts(counts)
                .analysisStatus(status)
                .lastIndexedAt(lastIndexed)
                .build();
    }
}