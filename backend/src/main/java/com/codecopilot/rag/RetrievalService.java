package com.codecopilot.rag;

import com.codecopilot.code.repo.CodeClassRepository;
import com.codecopilot.code.repo.CodeMethodRepository;
import com.codecopilot.code.repo.CodeReferenceRepository;
import com.codecopilot.code.repo.RepositoryFileRepository;
import com.codecopilot.config.AppProperties;
import com.codecopilot.embedding.CodeVectorStore;
import com.codecopilot.embedding.EmbeddingService;
import com.codecopilot.embedding.JpaCodeVectorStore;
import com.codecopilot.search.SearchService;
import com.codecopilot.search.dto.SearchRequest;
import com.codecopilot.search.dto.SearchResultDto;
import com.codecopilot.code.entity.CodeClass;
import com.codecopilot.code.entity.CodeMethod;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Retrieves grounding evidence for a user question: exact symbol matches,
 * API endpoint matches, vector hits, and related references. Produces both a
 * human-readable context block and structured source citations.
 */
@Service
public class RetrievalService {

    public record SourceDoc(
            String filePath,
            String className,
            String methodName,
            int startLine,
            int endLine,
            String snippet,
            String sourceType,
            double score) {
    }

    public record RetrievalResult(List<SourceDoc> sources, String context, int tokens) {
    }

    private final SearchService searchService;
    private final CodeVectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final CodeClassRepository classRepository;
    private final CodeMethodRepository methodRepository;
    private final CodeReferenceRepository referenceRepository;
    private final RepositoryFileRepository fileRepository;
    private final AppProperties properties;

    public RetrievalService(SearchService searchService, CodeVectorStore vectorStore,
                            EmbeddingService embeddingService, CodeClassRepository classRepository,
                            CodeMethodRepository methodRepository, CodeReferenceRepository referenceRepository,
                            RepositoryFileRepository fileRepository, AppProperties properties) {
        this.searchService = searchService;
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
        this.classRepository = classRepository;
        this.methodRepository = methodRepository;
        this.referenceRepository = referenceRepository;
        this.fileRepository = fileRepository;
        this.properties = properties;
    }

    public RetrievalResult retrieve(Long projectId, List<Long> repoIds, String question) {
        Map<String, SourceDoc> docs = new LinkedHashMap<>();

        // Exact and substring hits
        SearchRequest req = new SearchRequest();
        req.setQuery(question);
        req.setTopK(8);
        req.setRepositoryIds(repoIds == null || repoIds.isEmpty() ? null : repoIds);
        for (SearchResultDto r : searchService.search(projectId, req)) {
            add(docs, new SourceDoc(
                    r.getFilePath(), r.getClassName(), r.getMethodName(),
                    r.getStartLine(), r.getEndLine(), r.getSnippet(),
                    r.getSourceType() != null ? r.getSourceType() : r.getMatchType(), r.getScore()));
        }

        // Semantic retrieval over chunks
        float[] queryVec = embeddingService.embed(question);
        List<JpaCodeVectorStore.SearchHit> hits = vectorStore.search(queryVec, projectId, null,
                properties.getAi().getMaxContextChunks());
        for (JpaCodeVectorStore.SearchHit h : hits) {
            add(docs, new SourceDoc(h.filePath(), h.className(), h.methodName(),
                    h.startLine(), h.endLine(), h.content(), h.chunkType(), h.score()));
        }

        // Related references for classes mentioned in the question
        for (CodeClass c : classesNamedIn(projectId, repoIds, question)) {
            for (var ref : referenceRepository.findByProjectIdAndTargetName(projectId, c.getName())) {
                add(docs, new SourceDoc(
                        filePathOf(ref.getFileId()),
                        ref.getSourceClassFq(),
                        ref.getSourceMethodName(),
                        -1, -1, ref.getType() + " -> " + c.getName(),
                        "REFERENCE", 0.6));
            }
        }

        List<SourceDoc> sorted = docs.values().stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(properties.getAi().getMaxContextChunks() + 6)
                .toList();

        StringBuilder context = new StringBuilder();
        int idx = 1;
        for (SourceDoc d : sorted) {
            context.append(String.format("[%d] %s:%s:%s (%d-%d)%n%s%n%n",
                    idx, d.filePath(),
                    d.className() == null ? "" : d.className(),
                    d.methodName() == null ? "" : d.methodName(),
                    d.startLine(), d.endLine(),
                    d.snippet() == null ? "" : d.snippet().trim()));
            idx++;
        }
        return new RetrievalResult(sorted, context.toString(), (int) (context.length() / 4));
    }

    private void add(Map<String, SourceDoc> map, SourceDoc doc) {
        String key = doc.filePath() + ":" + doc.className() + ":" + doc.methodName() + ":" + doc.startLine() + ":" + doc.endLine();
        SourceDoc existing = map.get(key);
        if (existing == null || existing.score() < doc.score()) {
            map.put(key, doc);
        }
    }

    private List<CodeClass> classesNamedIn(Long projectId, List<Long> repoIds, String question) {
        String q = question.toLowerCase(Locale.ROOT);
        List<CodeClass> classes = repoIds == null || repoIds.isEmpty()
                ? classRepository.findAll().stream()
                .filter(c -> c.getProjectId().equals(projectId)).toList()
                : repoIds.stream()
                .flatMap(rid -> classRepository.findByProjectIdAndRepositoryId(projectId, rid).stream())
                .toList();
        return classes.stream()
                .filter(c -> q.contains(c.getName().toLowerCase(Locale.ROOT)))
                .limit(10)
                .toList();
    }

    private String filePathOf(Long fileId) {
        if (fileId == null) {
            return "";
        }
        return fileRepository.findById(fileId).map(f -> f.getPath()).orElse("");
    }
}
