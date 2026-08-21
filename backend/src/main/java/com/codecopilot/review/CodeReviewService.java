package com.codecopilot.review;

import com.codecopilot.ai.AiService;
import com.codecopilot.ai.prompts.Prompts;
import com.codecopilot.code.entity.RepositoryFile;
import com.codecopilot.code.repo.RepositoryFileRepository;
import com.codecopilot.common.exception.BadRequestException;
import com.codecopilot.git.GitIntelligenceService;
import com.codecopilot.git.GitIntelligenceService.DiffResult;
import com.codecopilot.project.ProjectAccessService;
import com.codecopilot.project.ProjectRepository;
import com.codecopilot.project.Project;
import com.codecopilot.review.dto.ReviewRequest;
import com.codecopilot.review.dto.ReviewResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CodeReviewService {

    private final AiService aiService;
    private final ProjectAccessService accessService;
    private final ProjectRepository projectRepository;
    private final RepositoryFileRepository fileRepository;
    private final GitIntelligenceService gitService;
    private final ObjectMapper objectMapper;

    public CodeReviewService(AiService aiService, ProjectAccessService accessService,
                             ProjectRepository projectRepository, RepositoryFileRepository fileRepository,
                             GitIntelligenceService gitService, ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.accessService = accessService;
        this.projectRepository = projectRepository;
        this.fileRepository = fileRepository;
        this.gitService = gitService;
        this.objectMapper = objectMapper;
    }

    public ReviewResponse review(Long projectId, ReviewRequest request) {
        accessService.requireView(projectId);
        Long repoId = request.getRepositoryId();
        if (repoId == null && request.getRepositoryIds() != null && !request.getRepositoryIds().isEmpty()) {
            repoId = request.getRepositoryIds().get(0);
        }

        String target = switch (parseTarget(request.getTargetType())) {
            case FILE -> loadFile(request);
            case COMMIT -> gitService.diff(projectId, repoId, request.getCommitId()).unifiedDiff();
            case PR, BRANCH, DIFF -> request.getDiff() == null ? "" : request.getDiff();
        };
        if (target == null || target.isBlank()) {
            throw new BadRequestException("No review target content available. Provide a file path, commit id or diff.");
        }

        String projectName = projectRepository.findById(projectId).map(Project::getName).orElse("?");
        String system = Prompts.codeReviewSystem(projectName);
        String userPrompt = "Review the following code:\n\n```\n" + target + "\n```\n\n"
                + "Return ONLY a JSON array of findings. Each finding must have keys: "
                + "severity, category, confidence, location, title, detail, suggestion. "
                + "If there are no findings, return [].\n\nAfter the JSON, on a new line, "
                + "start with 'SUMMARY:' followed by a 2-3 sentence summary of the review.";

        String raw = aiService.chat(system, userPrompt);
        List<ReviewResponse.Finding> findings = parseFindings(raw);
        String summary = extractSummary(raw);
        return ReviewResponse.builder()
                .summary(summary)
                .findings(findings)
                .raw(raw)
                .build();
    }

    private String loadFile(ReviewRequest request) {
        if (request.getFilePath() == null || request.getFilePath().isBlank()) {
            return "";
        }
        List<RepositoryFile> files = fileRepository.findAll().stream()
                .filter(f -> f.getPath().endsWith(request.getFilePath()))
                .toList();
        return files.isEmpty() ? "" : files.get(0).getContent();
    }

    private ReviewRequest.TargetType parseTarget(String t) {
        try {
            return ReviewRequest.TargetType.valueOf(t.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("Unsupported review target type: " + t);
        }
    }

    private List<ReviewResponse.Finding> parseFindings(String raw) {
        List<ReviewResponse.Finding> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            String json = raw.substring(start, end + 1);
            try {
                return objectMapper.readerForListOf(ReviewResponse.Finding.class).readValue(json);
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private String extractSummary(String raw) {
        if (raw == null) return "";
        int idx = raw.indexOf("SUMMARY:");
        if (idx < 0) return "";
        return raw.substring(idx + 8).trim();
    }
}