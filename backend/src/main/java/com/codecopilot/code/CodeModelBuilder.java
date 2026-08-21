package com.codecopilot.code;

import com.codecopilot.code.entity.CodeClass;
import com.codecopilot.code.entity.CodeDependency;
import com.codecopilot.code.entity.CodeField;
import com.codecopilot.code.entity.CodeMethod;
import com.codecopilot.code.entity.CodePackage;
import com.codecopilot.code.entity.CodeReference;
import com.codecopilot.code.entity.RepositoryFile;
import com.codecopilot.code.repo.CodeClassRepository;
import com.codecopilot.code.repo.CodeDependencyRepository;
import com.codecopilot.code.repo.CodeFieldRepository;
import com.codecopilot.code.repo.CodeMethodRepository;
import com.codecopilot.code.repo.CodePackageRepository;
import com.codecopilot.code.repo.CodeReferenceRepository;
import com.codecopilot.code.repo.RepositoryFileRepository;
import com.codecopilot.parser.JavaSourceParser;
import com.codecopilot.parser.ParsedSourceFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CodeModelBuilder {

    private static final Logger log = LoggerFactory.getLogger(CodeModelBuilder.class);

    private final RepositoryFileRepository fileRepository;
    private final CodePackageRepository packageRepository;
    private final CodeClassRepository classRepository;
    private final CodeMethodRepository methodRepository;
    private final CodeFieldRepository fieldRepository;
    private final CodeDependencyRepository dependencyRepository;
    private final CodeReferenceRepository referenceRepository;
    private final ObjectMapper objectMapper;

    public CodeModelBuilder(RepositoryFileRepository fileRepository, CodePackageRepository packageRepository,
                            CodeClassRepository classRepository, CodeMethodRepository methodRepository,
                            CodeFieldRepository fieldRepository, CodeDependencyRepository dependencyRepository,
                            CodeReferenceRepository referenceRepository, ObjectMapper objectMapper) {
        this.fileRepository = fileRepository;
        this.packageRepository = packageRepository;
        this.classRepository = classRepository;
        this.methodRepository = methodRepository;
        this.fieldRepository = fieldRepository;
        this.dependencyRepository = dependencyRepository;
        this.referenceRepository = referenceRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists the structural model of one parsed Java file.
     */
    @Transactional
    public void persist(Long projectId, Long repositoryId, String relativePath, String language,
                        Path absolutePath, ParsedSourceFile parsed) {
        RepositoryFile file = fileRepository
                .findByProjectIdAndRepositoryIdAndPath(projectId, repositoryId, relativePath)
                .orElseGet(() -> {
                    RepositoryFile f = new RepositoryFile();
                    f.setProjectId(projectId);
                    f.setRepositoryId(repositoryId);
                    f.setPath(relativePath);
                    f.setFileName(relativePath.substring(relativePath.lastIndexOf('/') + 1));
                    f.setLanguage(language);
                    return f;
                });
        file.setLineCount((int) parsed.types().stream()
                .mapToInt(ParsedSourceFile.ParsedType::endLine)
                .max().orElse(0));
        if (absolutePath != null && Files.exists(absolutePath)) {
            try {
                byte[] bytes = Files.readAllBytes(absolutePath);
                file.setSizeBytes(bytes.length);
                file.setContent(new String(bytes, StandardCharsets.UTF_8));
                file.setChecksum(sha256(bytes));
            } catch (Exception e) {
                log.warn("Cannot read content of {}", relativePath);
            }
        }
        fileRepository.save(file);

        Map<String, Long> packageIds = new HashMap<>();
        Map<String, Long> classIds = new HashMap<>();
        Map<String, ParsedSourceFile.ParsedType> typeByName = parsed.types().stream()
                .collect(Collectors.toMap(ParsedSourceFile.ParsedType::fqName, t -> t, (a, b) -> a));

        for (ParsedSourceFile.ParsedType t : parsed.types()) {
            Long packageId = null;
            if (parsed.packageName() != null && !parsed.packageName().isBlank()) {
                packageId = packageIds.computeIfAbsent(parsed.packageName(), pn -> {
                    CodePackage p = packageRepository
                            .findByProjectIdAndRepositoryIdAndName(projectId, repositoryId, pn)
                            .orElseGet(() -> {
                                CodePackage np = new CodePackage();
                                np.setProjectId(projectId);
                                np.setRepositoryId(repositoryId);
                                np.setName(pn);
                                np.setFileId(file.getId());
                                return np;
                            });
                    return packageRepository.save(p).getId();
                });
            }
            CodeClass cc = new CodeClass();
            cc.setProjectId(projectId);
            cc.setRepositoryId(repositoryId);
            cc.setFileId(file.getId());
            cc.setPackageId(packageId);
            cc.setName(t.name());
            cc.setFqName(t.fqName());
            cc.setKind(t.kind().name());
            cc.setAnnotations(json(t.annotations()));
            cc.setModifiers(json(t.modifiers()));
            cc.setParentClass(t.parentClass());
            cc.setInterfaces(json(t.interfaces()));
            cc.setStartLine(t.startLine());
            cc.setEndLine(t.endLine());
            cc = classRepository.save(cc);
            classIds.put(t.fqName(), cc.getId());

            for (ParsedSourceFile.ParsedField f : t.fields()) {
                CodeField cf = new CodeField();
                cf.setProjectId(projectId);
                cf.setRepositoryId(repositoryId);
                cf.setFileId(file.getId());
                cf.setClassId(cc.getId());
                cf.setName(f.name());
                cf.setType(f.type());
                cf.setModifiers(json(f.modifiers()));
                cf.setAnnotations(json(f.annotations()));
                cf.setStartLine(f.startLine());
                cf.setEndLine(f.endLine());
                fieldRepository.save(cf);
            }

            for (ParsedSourceFile.ParsedMethod m : t.methods()) {
                CodeMethod cm = new CodeMethod();
                cm.setProjectId(projectId);
                cm.setRepositoryId(repositoryId);
                cm.setFileId(file.getId());
                cm.setClassId(cc.getId());
                cm.setName(m.name());
                cm.setReturnType(m.returnType());
                cm.setParametersJson(json(m.parameters().stream()
                        .map(p -> Map.of("name", p.name(), "type", p.type()))
                        .toList()));
                cm.setAnnotations(json(m.annotations()));
                cm.setBody(m.body());
                cm.setStartLine(m.startLine());
                cm.setEndLine(m.endLine());
                cm.setConstructor(m.constructor());
                cm.setStatic(m.modifiers().contains("STATIC"));
                cm.setPublic(m.modifiers().contains("PUBLIC"));
                methodRepository.save(cm);

                // method calls -> references (with optimistic resolution within this repo)
                for (String call : m.methodCalls()) {
                    CodeReference ref = new CodeReference();
                    ref.setProjectId(projectId);
                    ref.setRepositoryId(repositoryId);
                    ref.setFileId(file.getId());
                    ref.setSourceClassId(cc.getId());
                    ref.setSourceMethodId(cm.getId());
                    ref.setSourceClassFq(t.fqName());
                    ref.setSourceMethodName(m.name());
                    ref.setTargetName(call);
                    ref.setType("METHOD_CALL");
                    ref.setLineNumber(m.startLine());
                    referenceRepository.save(ref);
                }
            }

            // type-level dependencies
            if (t.parentClass() != null && !t.parentClass().equals(t.name())) {
                String fq = resolve(t.parentClass(), typeByName, parsed);
                saveDependency(projectId, repositoryId, file.getId(), cc.getId(), t.fqName(), fq, "EXTENDS", t.startLine());
            }
            for (String iface : t.interfaces()) {
                String fq = resolve(iface, typeByName, parsed);
                saveDependency(projectId, repositoryId, file.getId(), cc.getId(), t.fqName(), fq, "IMPLEMENTS", t.startLine());
            }
            for (ParsedSourceFile.ParsedField f : t.fields()) {
                String fq = resolve(f.type(), typeByName, parsed);
                if (fq != null) {
                    saveDependency(projectId, repositoryId, file.getId(), cc.getId(), t.fqName(), fq, "FIELD", f.startLine());
                }
            }
            for (ParsedSourceFile.ParsedMethod m : t.methods()) {
                for (ParsedSourceFile.Parameter p : m.parameters()) {
                    String fq = resolve(p.type(), typeByName, parsed);
                    if (fq != null) {
                        saveDependency(projectId, repositoryId, file.getId(), cc.getId(), t.fqName(), fq, "PARAMETER", m.startLine());
                    }
                }
                String fq = resolve(m.returnType(), typeByName, parsed);
                if (fq != null) {
                    saveDependency(projectId, repositoryId, file.getId(), cc.getId(), t.fqName(), fq, "RETURN", m.startLine());
                }
            }
        }

        // Attach REST endpoint metadata to the corresponding method rows.
        for (ParsedSourceFile.ParsedApiEndpoint endpoint : parsed.endpoints()) {
            Long classId = findClassId(typeByName, classIds, endpoint.className());
            if (classId == null) {
                continue;
            }
            methodRepository.findByProjectIdAndRepositoryIdAndClassIdAndName(
                            projectId, repositoryId, classId, endpoint.methodName())
                    .ifPresent(cm -> {
                        cm.setHttpMethod(endpoint.httpMethod());
                        cm.setHttpPath(endpoint.path());
                        methodRepository.save(cm);
                    });
        }
    }

    private Long findClassId(Map<String, ParsedSourceFile.ParsedType> byFq,
                             Map<String, Long> classIds, String name) {
        for (Map.Entry<String, ParsedSourceFile.ParsedType> e : byFq.entrySet()) {
            if (e.getValue().name().equals(name)) {
                return classIds.get(e.getKey());
            }
        }
        return null;
    }

    private void saveDependency(Long projectId, Long repositoryId, Long fileId, Long sourceClassId,
                                String sourceFq, String targetFq, String type, int line) {
        if (targetFq == null || targetFq.equals(sourceFq)) {
            return;
        }
        CodeDependency dep = new CodeDependency();
        dep.setProjectId(projectId);
        dep.setRepositoryId(repositoryId);
        dep.setFileId(fileId);
        dep.setSourceClassId(sourceClassId);
        dep.setSourceClassFq(sourceFq);
        dep.setTargetClassFq(targetFq);
        dep.setType(type);
        dep.setLineNumber(line);
        dependencyRepository.save(dep);
    }

    /**
     * Resolves a (possibly simple) type name to a fully-qualified name using
     * classes defined within this scanned repo. Unresolved types stay unresolved.
     */
    private String resolve(String typeName, Map<String, ParsedSourceFile.ParsedType> byFq,
                           ParsedSourceFile parsed) {
        if (typeName == null || typeName.isBlank()) {
            return null;
        }
        String simple = typeName;
        if (simple.contains("<")) {
            simple = simple.substring(0, simple.indexOf('<'));
        }
        if (simple.contains(".")) {
            simple = simple.substring(simple.lastIndexOf('.') + 1);
        }
        if ("void".equals(simple)) {
            return null;
        }
        for (Map.Entry<String, ParsedSourceFile.ParsedType> e : byFq.entrySet()) {
            if (simple.equals(e.getValue().name())) {
                return e.getKey();
            }
        }
        // imports may give us exact mapping
        for (String imp : parsed.imports()) {
            if (imp.endsWith("." + simple)) {
                return imp;
            }
        }
        return null;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String sha256(byte[] content) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            var sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Long.toHexString(java.util.Arrays.hashCode(content));
        }
    }
}