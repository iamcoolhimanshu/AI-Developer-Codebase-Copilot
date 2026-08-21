package com.codecopilot.testing;

import com.codecopilot.ai.AiService;
import com.codecopilot.ai.prompts.Prompts;
import com.codecopilot.code.entity.CodeClass;
import com.codecopilot.code.entity.CodeMethod;
import com.codecopilot.code.entity.RepositoryFile;
import com.codecopilot.code.repo.CodeClassRepository;
import com.codecopilot.code.repo.CodeMethodRepository;
import com.codecopilot.code.repo.CodeReferenceRepository;
import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.project.ProjectAccessService;
import com.codecopilot.project.ProjectRepository;
import com.codecopilot.project.Project;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TestGeneratorService {

    private final AiService aiService;
    private final ProjectAccessService accessService;
    private final ProjectRepository projectRepository;
    private final CodeClassRepository classRepository;
    private final CodeMethodRepository methodRepository;
    private final CodeReferenceRepository referenceRepository;
    private final com.codecopilot.code.repo.RepositoryFileRepository fileRepository;

    public TestGeneratorService(AiService aiService, ProjectAccessService accessService,
                                ProjectRepository projectRepository, CodeClassRepository classRepository,
                                CodeMethodRepository methodRepository, CodeReferenceRepository referenceRepository,
                                com.codecopilot.code.repo.RepositoryFileRepository fileRepository) {
        this.aiService = aiService;
        this.accessService = accessService;
        this.projectRepository = projectRepository;
        this.classRepository = classRepository;
        this.methodRepository = methodRepository;
        this.referenceRepository = referenceRepository;
        this.fileRepository = fileRepository;
    }

    public GeneratedTests generate(Long projectId, Long classId, String methodName) {
        accessService.requireView(projectId);
        CodeClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class not found"));
        List<CodeMethod> methods = methodRepository.findByProjectIdAndRepositoryIdAndClassId(
                projectId, cls.getRepositoryId(), classId);

        List<CodeMethod> targets = methodName == null || methodName.isBlank()
                ? methods : methods.stream().filter(m -> m.getName().equals(methodName)).toList();
        if (targets.isEmpty()) {
            targets = methods;
        }

        StringBuilder source = new StringBuilder();
        source.append("Class: ").append(cls.getFqName()).append("\n");
        RepositoryFile file = fileRepository.findById(cls.getFileId()).orElse(null);
        if (file != null) {
            source.append("File: ").append(file.getPath()).append("\n\n");
        }
        for (CodeMethod m : targets) {
            source.append("```java\n");
            if (m.getBody() != null && !m.getBody().isBlank()) {
                source.append(m.getBody()).append("\n");
            } else {
                source.append("public ").append(m.getReturnType()).append(' ').append(m.getName())
                        .append('(').append(m.getParametersJson()).append(") { ... }\n");
            }
            source.append("```\n\n");
        }

        String projectName = projectRepository.findById(projectId).map(Project::getName).orElse("?");
        String system = Prompts.testGenerationSystem(projectName);
        String userPrompt = "Generate JUnit 5 + Mockito tests for the following source:\n\n" + source;
        String code = extractJavaCode(aiService.chat(system, userPrompt));

        return new GeneratedTests(cls.getName(), targets.stream().map(CodeMethod::getName).toList(),
                code, aiService.isConfigured() ? "grok" : null);
    }

    private String extractJavaCode(String text) {
        if (text == null) return "";
        int start = text.indexOf("```java");
        if (start < 0) start = text.indexOf("```");
        if (start < 0) return text.trim();
        int end = text.indexOf("```", start + 3);
        if (end < 0) end = text.length();
        return text.substring(text.indexOf('\n', start) + 1, end).trim();
    }

    public record GeneratedTests(String className, List<String> methods, String code, String model) {
    }
}