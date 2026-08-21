package com.codecopilot.patch;

import com.codecopilot.ai.AiService;
import com.codecopilot.ai.prompts.Prompts;
import com.codecopilot.code.entity.CodeClass;
import com.codecopilot.code.entity.RepositoryFile;
import com.codecopilot.code.repo.RepositoryFileRepository;
import com.codecopilot.common.exception.BadRequestException;
import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.common.security.SecurityUtils;
import com.codecopilot.audit.AuditService;
import com.codecopilot.patch.dto.PatchRequest;
import com.codecopilot.patch.entity.GeneratedPatch;
import com.codecopilot.project.Project;
import com.codecopilot.project.ProjectAccessService;
import com.codecopilot.project.ProjectRepository;
import com.codecopilot.repository.GitRepository;
import com.codecopilot.repository.GitRepositoryRepository;
import com.codecopilot.repository.RepositoryStorageService;
import com.codecopilot.search.SearchService;
import com.codecopilot.search.dto.SearchRequest;
import com.codecopilot.search.dto.SearchResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Safe code modification workflow: analyze -> generate patch -> developer
 * approval -> apply (via git apply) -> report. The AI can never modify code
 * without explicit human approval (enforced here plus audited).
 */
@Service
public class PatchService {

    private static final Logger log = LoggerFactory.getLogger(PatchService.class);

    private final GeneratedPatchRepository patchRepository;
    private final SearchService searchService;
    private final RepositoryFileRepository fileRepository;
    private final RepositoryStorageService storageService;
    private final GitRepositoryRepository repositoryRepository;
    private final AiService aiService;
    private final ProjectAccessService accessService;
    private final ProjectRepository projectRepository;
    private final AuditService auditService;

    public PatchService(GeneratedPatchRepository patchRepository, SearchService searchService,
                        RepositoryFileRepository fileRepository, RepositoryStorageService storageService,
                        GitRepositoryRepository repositoryRepository, AiService aiService,
                        ProjectAccessService accessService, ProjectRepository projectRepository,
                        AuditService auditService) {
        this.patchRepository = patchRepository;
        this.searchService = searchService;
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.repositoryRepository = repositoryRepository;
        this.aiService = aiService;
        this.accessService = accessService;
        this.projectRepository = projectRepository;
        this.auditService = auditService;
    }

    @Transactional
    public GeneratedPatch generate(Long projectId, PatchRequest request) {
        accessService.requireEdit(projectId);
        GitRepository repo = repositoryRepository.findByIdAndProjectId(request.getRepositoryId(), projectId)
                .orElseThrow(() -> new NotFoundException("Repository not found"));

        SearchRequest searchReq = new SearchRequest();
        searchReq.setQuery(request.getInstruction());
        searchReq.setTopK(6);
        searchReq.setRepositoryIds(List.of(repo.getId()));
        List<SearchResultDto> hits = searchService.search(projectId, searchReq);

        StringBuilder context = new StringBuilder();
        for (SearchResultDto h : hits) {
            RepositoryFile f = fileRepository.findAll().stream()
                    .filter(rf -> rf.getProjectId().equals(projectId) && rf.getPath().equals(h.getFilePath()))
                    .findFirst().orElse(null);
            if (f != null && f.getContent() != null) {
                context.append("File: ").append(f.getPath()).append("\n")
                        .append(f.getContent(), 0, Math.min(f.getContent().length(), 3000)).append("\n\n");
            }
        }
        if (context.isEmpty()) {
            throw new BadRequestException("No relevant source found for this instruction. Index the repository first.");
        }

        String projectName = projectRepository.findById(projectId).map(Project::getName).orElse("?");
        String system = Prompts.patchGenerationSystem(projectName);
        String userPrompt = "Instruction: " + request.getInstruction()
                + "\n\nRelevant source:\n" + context
                + "\n\nReturn ONLY the unified diff between --- and +++ headers, no extra prose.";

        String result = aiService.chat(system, userPrompt);

        GeneratedPatch patch = new GeneratedPatch();
        patch.setProjectId(projectId);
        patch.setRepositoryId(repo.getId());
        patch.setUserId(SecurityUtils.currentUserId());
        patch.setInstruction(request.getInstruction());
        patch.setDiff(result.trim());
        patch.setSummary(extractSummary(result));
        patch.setStatus("PENDING");
        patch = patchRepository.save(patch);
        auditService.log(projectId, "PATCH_GENERATED", "generated_patch:" + patch.getId(),
                request.getInstruction());
        return patch;
    }

    public List<GeneratedPatch> list(Long projectId) {
        accessService.requireView(projectId);
        return patchRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional
    public GeneratedPatch approve(Long projectId, Long patchId) {
        accessService.requireEdit(projectId);
        GeneratedPatch patch = requirePatch(projectId, patchId);
        patch.setStatus("APPROVED");
        patch.setApprovedAt(Instant.now());
        patch = patchRepository.save(patch);
        auditService.log(projectId, "PATCH_APPROVED", "generated_patch:" + patch.getId(), patch.getInstruction());
        return patch;
    }

    @Transactional
    public GeneratedPatch apply(Long projectId, Long patchId) {
        accessService.requireEdit(projectId);
        GeneratedPatch patch = requirePatch(projectId, patchId);
        if (!"APPROVED".equals(patch.getStatus())) {
            throw new BadRequestException("Patch must be approved before applying.");
        }
        GitRepository repo = repositoryRepository.findById(patch.getRepositoryId())
                .orElseThrow(() -> new NotFoundException("Repository not found"));
        Path dir = storageService.repoDirectory(repo);
        byte[] diffBytes = patch.getDiff().getBytes(StandardCharsets.UTF_8);
        runGitWithStdin(dir, diffBytes, "apply", "--check", "-");
        runGitWithStdin(dir, diffBytes, "apply", "-");
        patch.setStatus("APPLIED");
        patch.setAppliedAt(Instant.now());
        patch = patchRepository.save(patch);
        auditService.log(projectId, "PATCH_APPLIED", "generated_patch:" + patch.getId(), patch.getInstruction());
        return patch;
    }

    @Transactional
    public GeneratedPatch reject(Long projectId, Long patchId) {
        accessService.requireEdit(projectId);
        GeneratedPatch patch = requirePatch(projectId, patchId);
        patch.setStatus("REJECTED");
        patch = patchRepository.save(patch);
        auditService.log(projectId, "PATCH_REJECTED", "generated_patch:" + patch.getId(), patch.getInstruction());
        return patch;
    }

    private GeneratedPatch requirePatch(Long projectId, Long patchId) {
        GeneratedPatch patch = patchRepository.findById(patchId)
                .orElseThrow(() -> new NotFoundException("Patch not found"));
        if (!patch.getProjectId().equals(projectId)) {
            throw new NotFoundException("Patch not found");
        }
        return patch;
    }

    private String extractSummary(String diff) {
        String[] lines = diff.split("\n");
        return java.util.Arrays.stream(lines)
                .filter(l -> l.startsWith("+") && !l.startsWith("+++") && !l.startsWith("+-"))
                .limit(6)
                .collect(Collectors.joining("\n"));
    }

    private void runGitWithStdin(Path dir, byte[] stdin, String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git");
            pb.command().addAll(java.util.List.of(args));
            pb.directory(dir.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getOutputStream().write(stdin);
            p.getOutputStream().close();
            int code = p.waitFor();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (code != 0) {
                throw new BadRequestException("git failed: " + out);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("git interrupted");
        } catch (java.io.IOException e) {
            throw new BadRequestException("git unavailable: " + e.getMessage());
        }
    }
}