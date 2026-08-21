package com.codecopilot.code;

import com.codecopilot.code.entity.CodeClass;
import com.codecopilot.code.entity.CodeDependency;
import com.codecopilot.code.entity.CodeField;
import com.codecopilot.code.entity.CodeMethod;
import com.codecopilot.code.entity.CodeReference;
import com.codecopilot.code.entity.RepositoryFile;
import com.codecopilot.code.repo.CodeClassRepository;
import com.codecopilot.code.repo.CodeDependencyRepository;
import com.codecopilot.code.repo.CodeFieldRepository;
import com.codecopilot.code.repo.CodeMethodRepository;
import com.codecopilot.code.repo.CodeReferenceRepository;
import com.codecopilot.code.repo.RepositoryFileRepository;
import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.project.ProjectAccessService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CodeExplorerService {

    private final RepositoryFileRepository fileRepository;
    private final CodeClassRepository classRepository;
    private final CodeMethodRepository methodRepository;
    private final CodeFieldRepository fieldRepository;
    private final CodeDependencyRepository dependencyRepository;
    private final CodeReferenceRepository referenceRepository;
    private final ProjectAccessService accessService;

    public CodeExplorerService(RepositoryFileRepository fileRepository, CodeClassRepository classRepository,
                               CodeMethodRepository methodRepository, CodeFieldRepository fieldRepository,
                               CodeDependencyRepository dependencyRepository, CodeReferenceRepository referenceRepository,
                               ProjectAccessService accessService) {
        this.fileRepository = fileRepository;
        this.classRepository = classRepository;
        this.methodRepository = methodRepository;
        this.fieldRepository = fieldRepository;
        this.dependencyRepository = dependencyRepository;
        this.referenceRepository = referenceRepository;
        this.accessService = accessService;
    }

    public List<RepositoryFile> files(Long projectId, Long repoId) {
        accessService.requireView(projectId);
        return repoId == null
                ? fileRepository.findAll().stream().filter(f -> f.getProjectId().equals(projectId)).toList()
                : fileRepository.findByProjectIdAndRepositoryIdOrderByPath(projectId, repoId);
    }

    public RepositoryFile fileContent(Long projectId, Long fileId) {
        accessService.requireView(projectId);
        RepositoryFile f = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));
        if (!f.getProjectId().equals(projectId)) {
            throw new NotFoundException("File not found");
        }
        return f;
    }

    public List<CodeClass> classes(Long projectId, Long repoId) {
        accessService.requireView(projectId);
        return repoId == null
                ? classRepository.findAll().stream().filter(c -> c.getProjectId().equals(projectId)).toList()
                : classRepository.findByProjectIdAndRepositoryId(projectId, repoId);
    }

    public CodeClass classDetail(Long projectId, Long classId) {
        accessService.requireView(projectId);
        CodeClass c = classRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class not found"));
        if (!c.getProjectId().equals(projectId)) {
            throw new NotFoundException("Class not found");
        }
        return c;
    }

    public List<CodeMethod> methodsOf(Long projectId, Long classId) {
        accessService.requireView(projectId);
        CodeClass c = classDetail(projectId, classId);
        return methodRepository.findByProjectIdAndRepositoryIdAndClassId(projectId, c.getRepositoryId(), classId);
    }

    public List<CodeField> fieldsOf(Long projectId, Long classId) {
        accessService.requireView(projectId);
        return fieldRepository.findByProjectIdAndRepositoryIdAndClassId(projectId,
                classDetail(projectId, classId).getRepositoryId(), classId);
    }

    public List<CodeMethod> apiEndpoints(Long projectId, Long repoId) {
        accessService.requireView(projectId);
        return repoId == null
                ? methodRepository.findByProjectIdAndHttpPathNotNull(projectId)
                : methodRepository.findByProjectIdAndRepositoryIdAndHttpPathNotNull(projectId, repoId);
    }

    public List<CodeDependency> dependencies(Long projectId, Long repoId) {
        accessService.requireView(projectId);
        return repoId == null
                ? dependencyRepository.findByProjectId(projectId)
                : dependencyRepository.findByProjectIdAndRepositoryId(projectId, repoId);
    }

    public ArchitectureGraph architecture(Long projectId, Long repoId) {
        accessService.requireView(projectId);
        List<CodeClass> classes = classes(projectId, repoId);
        List<CodeDependency> deps = dependencies(projectId, repoId);

        Map<Long, CodeClass> byId = classes.stream().collect(Collectors.toMap(CodeClass::getId, c -> c));
        Map<String, Long> byFq = classes.stream().collect(Collectors.toMap(CodeClass::getFqName, CodeClass::getId, (a, b) -> a));

        List<ArchitectureGraph.Node> nodes = new ArrayList<>();
        for (CodeClass c : classes) {
            nodes.add(new ArchitectureGraph.Node(c.getId(), c.getName(), c.getFqName(), stereotype(c), c.getKind(), pathOf(c.getFileId())));
        }

        List<ArchitectureGraph.Edge> edges = new ArrayList<>();
        var seen = new java.util.HashSet<String>();
        for (CodeDependency d : deps) {
            Long src = byFq.get(d.getSourceClassFq());
            Long tgt = byFq.get(d.getTargetClassFq());
            if (src == null || tgt == null) {
                continue;
            }
            String key = src + "->" + tgt;
            if (seen.add(key)) {
                edges.add(new ArchitectureGraph.Edge(src, tgt, d.getType()));
            }
        }
        return new ArchitectureGraph(nodes, edges, countByStereotype(nodes));
    }

    public record ArchitectureGraph(List<Node> nodes, List<Edge> edges, Map<String, Long> stereotypes) {
        public record Node(Long id, String name, String fqName, String stereotype, String kind, String filePath) {
        }

        public record Edge(Long source, Long target, String type) {
        }
    }

    private Map<String, Long> countByStereotype(List<ArchitectureGraph.Node> nodes) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (ArchitectureGraph.Node n : nodes) {
            out.merge(n.stereotype(), 1L, Long::sum);
        }
        return out;
    }

    public String stereotype(CodeClass c) {
        String anns = c.getAnnotations() == null ? "" : c.getAnnotations();
        if (anns.contains("RestController") || anns.contains("Controller")) {
            return "Controller";
        }
        if (anns.contains("RestControllerAdvice") || anns.contains("ControllerAdvice")) {
            return "ControllerAdvice";
        }
        if (anns.contains("Service")) {
            return "Service";
        }
        if (anns.contains("Repository")) {
            return "Repository";
        }
        if (anns.contains("Entity")) {
            return "Entity";
        }
        if (anns.contains("Configuration")) {
            return "Configuration";
        }
        if (anns.contains("Component")) {
            return "Component";
        }
        if ("INTERFACE".equals(c.getKind())) {
            return "Interface";
        }
        if ("ENUM".equals(c.getKind()) || "RECORD".equals(c.getKind())) {
            return "Model";
        }
        return "Other";
    }

    public List<WhereUsedResult> whereUsed(Long projectId, String symbol, String type) {
        accessService.requireView(projectId);
        List<WhereUsedResult> out = new ArrayList<>();
        if (type == null || type.equals("method")) {
            for (CodeReference ref : referenceRepository.findByProjectIdAndTargetName(projectId, symbol)) {
                out.add(new WhereUsedResult(
                        ref.getSourceClassFq(), ref.getSourceMethodName(),
                        ref.getType(), ref.getLineNumber(), pathOf(ref.getFileId()), ref.getProjectId()));
            }
        }
        if (type == null || type.equals("class") || type.equals("type")) {
            for (CodeDependency dep : dependencyRepository.findByProjectIdAndTargetClassFq(projectId, symbol)) {
                out.add(new WhereUsedResult(
                        dep.getSourceClassFq(), null, dep.getType(), dep.getLineNumber(), pathOf(dep.getFileId()), dep.getProjectId()));
            }
            for (CodeReference ref : referenceRepository.findByProjectIdAndTargetName(projectId, symbol)) {
                out.add(new WhereUsedResult(
                        ref.getSourceClassFq(), ref.getSourceMethodName(), ref.getType(), ref.getLineNumber(), pathOf(ref.getFileId()), ref.getProjectId()));
            }
        }
        return out;
    }

    public List<CodeClass> searchClasses(Long projectId, String query) {
        accessService.requireView(projectId);
        String q = query == null ? "" : query.trim().toLowerCase();
        return classRepository.findAll().stream()
                .filter(c -> c.getProjectId().equals(projectId))
                .filter(c -> q.isEmpty() || c.getName().toLowerCase().contains(q) || c.getFqName().toLowerCase().contains(q))
                .toList();
    }

    private String pathOf(Long fileId) {
        if (fileId == null) {
            return "";
        }
        return fileRepository.findById(fileId).map(RepositoryFile::getPath).orElse("");
    }

    public record WhereUsedResult(String classFqName, String methodName, String relationType, int line, String filePath, Long projectId) {
    }
}