package com.codecopilot.ai.tools;

import com.codecopilot.code.CodeExplorerService;
import com.codecopilot.code.entity.CodeClass;
import com.codecopilot.code.entity.CodeMethod;
import com.codecopilot.code.entity.RepositoryFile;
import com.codecopilot.git.GitIntelligenceService;
import com.codecopilot.project.ProjectAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class CodeIntelligenceTools {

    private static final Logger log = LoggerFactory.getLogger(CodeIntelligenceTools.class);

    private final CodeExplorerService explorer;
    private final GitIntelligenceService gitService;
    private final ProjectAccessService accessService;
    private final ToolExecutionRepository executionRepository;
    private final com.codecopilot.code.repo.RepositoryFileRepository fileRepository;
    private final com.codecopilot.repository.GitRepositoryRepository repositoryRepository;

    public CodeIntelligenceTools(CodeExplorerService explorer, GitIntelligenceService gitService,
                                 ProjectAccessService accessService, ToolExecutionRepository executionRepository,
                                 com.codecopilot.code.repo.RepositoryFileRepository fileRepository,
                                 com.codecopilot.repository.GitRepositoryRepository repositoryRepository) {
        this.explorer = explorer;
        this.gitService = gitService;
        this.accessService = accessService;
        this.executionRepository = executionRepository;
        this.fileRepository = fileRepository;
        this.repositoryRepository = repositoryRepository;
    }

    private void audit(String tool, String input) {
        try {
            var exec = new com.codecopilot.ai.tools.entity.ToolExecution();
            exec.setProjectId(CurrentProjectContext.projectId());
            exec.setUserId(CurrentProjectContext.userId());
            exec.setToolName(tool);
            exec.setInputJson(input);
            exec.setReadOnly(true);
            exec.setAuthorized(true);
            executionRepository.save(exec);
        } catch (Exception e) {
            log.warn("Tool audit failed for {}", tool, e);
        }
    }

    private void assertProject() {
        Long projectId = CurrentProjectContext.projectId();
        if (projectId == null) {
            throw new IllegalStateException("Tool invoked outside a project context");
        }
        accessService.requireView(projectId);
    }

    @Tool(description = "Searches the indexed codebase for classes and methods whose names contain the query.")
    public String searchCode(@ToolParam(description = "search string, e.g. a symbol or file name fragment") String query) {
        assertProject();
        audit("searchCode", query);
        Long projectId = CurrentProjectContext.projectId();
        var classes = explorer.searchClasses(projectId, query);
        StringBuilder sb = new StringBuilder();
        classes.stream().limit(25).forEach(c -> sb
                .append(c.getFqName()).append(" (").append(c.getKind()).append(") at ")
                .append(pathOf(c)).append(':').append(c.getStartLine()).append('\n'));
        return sb.isEmpty() ? "No matches." : sb.toString();
    }

    @Tool(description = "Returns the full source content of a file inside the current project.")
    public String getFile(
            @ToolParam(description = "file path relative to repository root, e.g. src/main/java/com/x/Service.java") String path) {
        assertProject();
        audit("getFile", path);
        Long projectId = CurrentProjectContext.projectId();
        return explorer.files(projectId, null).stream()
                .filter(f -> f.getPath().endsWith(path))
                .findFirst()
                .map(f -> "File: " + f.getPath() + "\n```\n" + f.getContent() + "\n```")
                .orElse("File not found.");
    }

    @Tool(description = "Finds a class by name and returns its declaration info and method list.")
    public String findClass(@ToolParam(description = "class simple or fully qualified name") String className) {
        assertProject();
        audit("findClass", className);
        Long projectId = CurrentProjectContext.projectId();
        List<CodeClass> matches = explorer.searchClasses(projectId, className).stream()
                .filter(c -> c.getName().equalsIgnoreCase(className) || c.getFqName().equalsIgnoreCase(className))
                .toList();
        if (matches.isEmpty()) {
            return "No class found named " + className;
        }
        CodeClass c = matches.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append(c.getKind()).append(' ').append(c.getFqName()).append('\n')
                .append("Annotations: ").append(c.getAnnotations()).append('\n')
                .append("Path: ").append(pathOf(c)).append(' ').append(c.getStartLine()).append('-').append(c.getEndLine()).append('\n');
        explorer.methodsOf(projectId, c.getId()).forEach(m ->
                sb.append("  ").append(m.getReturnType()).append(' ').append(m.getName()).append("(...)  [")
                        .append(m.getStartLine()).append('-').append(m.getEndLine()).append("]\n"));
        return sb.toString();
    }

    @Tool(description = "Finds a method by name and returns where it is declared with its signature and body.")
    public String findMethod(@ToolParam(description = "method name") String methodName) {
        assertProject();
        audit("findMethod", methodName);
        Long projectId = CurrentProjectContext.projectId();
        Long repositoryId = null;
        StringBuilder sb = new StringBuilder();
        for (CodeClass c : explorer.classes(projectId, null)) {
            for (CodeMethod m : explorer.methodsOf(projectId, c.getId())) {
                if (m.getName().equals(methodName)) {
                    sb.append(c.getFqName()).append('.')
                            .append(m.getName()).append(" at ").append(pathOf(c)).append(':')
                            .append(m.getStartLine()).append("\n");
                    if (m.getBody() != null) {
                        sb.append("```\n").append(m.getBody().length() > 1500 ? m.getBody().substring(0, 1500) : m.getBody()).append("\n```\n\n");
                    }
                }
            }
        }
        return sb.isEmpty() ? "Method not found." : sb.toString();
    }

    @Tool(description = "Finds where a symbol (class or method) is referenced across the codebase.")
    public String findReferences(@ToolParam(description = "symbol name, e.g. JwtService or calculateDiscount") String symbol) {
        assertProject();
        audit("findReferences", symbol);
        Long projectId = CurrentProjectContext.projectId();
        var used = explorer.whereUsed(projectId, symbol, null);
        if (used.isEmpty()) {
            return "No references found for " + symbol;
        }
        return used.stream().distinct()
                .map(u -> u.classFqName() + "." + (u.methodName() == null ? "" : u.methodName())
                        + " (" + u.relationType() + ") at " + u.filePath() + ":" + u.line())
                .distinct()
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Finds REST API endpoints matching a text fragment (path or method).")
    public String findApiEndpoint(@ToolParam(description = "path or keyword fragment, e.g. /orders or login") String fragment) {
        assertProject();
        audit("findApiEndpoint", fragment);
        Long projectId = CurrentProjectContext.projectId();
        StringBuilder sb = new StringBuilder();
        explorer.apiEndpoints(projectId, null).stream()
                .filter(m -> (m.getHttpPath() != null && m.getHttpPath().contains(fragment))
                        || m.getName().toLowerCase().contains(fragment.toLowerCase()))
                .forEach(m -> sb.append(m.getHttpMethod()).append(' ').append(m.getHttpPath())
                        .append(" -> ").append(classNameOf(m)).append('.').append(m.getName()).append('\n'));
        return sb.isEmpty() ? "No endpoints matched." : sb.toString();
    }

    @Tool(description = "Lists recent git commits for the current project repository.")
    public String getGitHistory(@ToolParam(description = "optional repository id; omit to use the first repository") String repositoryId) {
        assertProject();
        audit("getGitHistory", repositoryId);
        Long projectId = CurrentProjectContext.projectId();
        Long rid = repositoryId != null && !repositoryId.isBlank() ? Long.parseLong(repositoryId) : firstRepoId(projectId);
        try {
            var commits = gitService.commits(projectId, rid, null, 10);
            return commits.stream().map(c -> c.shortId() + " " + c.author() + " " + c.message()).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "Git history unavailable: " + e.getMessage();
        }
    }

    @Tool(description = "Lists test classes/methods that touch a given class name.")
    public String findTests(@ToolParam(description = "class name to find tests for") String className) {
        assertProject();
        audit("findTests", className);
        Long projectId = CurrentProjectContext.projectId();
        var tests = explorer.searchClasses(projectId, "Test").stream().toList();
        StringBuilder sb = new StringBuilder();
        for (CodeClass t : tests) {
            for (CodeMethod m : explorer.methodsOf(projectId, t.getId())) {
                if (t.getName().contains(className) || m.getName().toLowerCase().contains(className.toLowerCase())
                        || t.getName().toLowerCase().contains(className.toLowerCase())) {
                    sb.append(t.getFqName()).append('.').append(m.getName())
                            .append(" at ").append(pathOf(t)).append('\n');
                }
            }
        }
        return sb.isEmpty() ? "No tests found." : sb.toString();
    }

    private String pathOf(CodeClass c) {
        if (c.getFileId() == null) {
            return "";
        }
        return fileRepository.findById(c.getFileId()).map(RepositoryFile::getPath).orElse("");
    }

    private String classNameOf(CodeMethod m) {
        return "?";
    }

    private Long firstRepoId(Long projectId) {
        return repositoryRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .findFirst().map(com.codecopilot.repository.GitRepository::getId)
                .orElse(null);
    }
}