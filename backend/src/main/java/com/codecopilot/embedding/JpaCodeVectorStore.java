package com.codecopilot.embedding;

import com.codecopilot.embedding.entity.CodeChunk;
import com.codecopilot.embedding.entity.CodeEmbedding;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Vector storage abstraction. Current implementation keeps vectors in MySQL
 * (JSON column) and computes cosine similarity in Java over the filtered
 * candidate set — the abstraction allows a native VECTOR store to replace this
 * later without touching the rest of the application.
 */
@Service
public class JpaCodeVectorStore implements CodeVectorStore {

    public record SearchHit(
            Long chunkId,
            double score,
            String filePath,
            String className,
            String methodName,
            String chunkType,
            int startLine,
            int endLine,
            String content) {
    }

    private final CodeEmbeddingRepository embeddingRepository;
    private final CodeChunkRepository chunkRepository;
    private final ObjectMapper objectMapper;

    public JpaCodeVectorStore(CodeEmbeddingRepository embeddingRepository,
                              CodeChunkRepository chunkRepository,
                              ObjectMapper objectMapper) {
        this.embeddingRepository = embeddingRepository;
        this.chunkRepository = chunkRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void upsert(List<CodeEmbedding> embeddings) {
        embeddingRepository.saveAll(embeddings);
    }

    @Override
    public void deleteByRepository(Long repositoryId) {
        embeddingRepository.deleteByRepositoryId(repositoryId);
    }

    @Override
    public List<SearchHit> search(float[] query, Long projectId, List<Long> repositoryIds, int topK) {
        List<CodeEmbedding> candidates;
        if (repositoryIds == null || repositoryIds.isEmpty()) {
            candidates = embeddingRepository.findAll().stream()
                    .filter(e -> e.getProjectId().equals(projectId))
                    .toList();
        } else {
            candidates = new ArrayList<>();
            for (Long rid : repositoryIds) {
                candidates.addAll(embeddingRepository.findByProjectIdAndRepositoryId(projectId, rid));
            }
        }
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<long[]> scores = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            float[] vec = toVector(candidates.get(i).getVectorJson());
            if (vec == null) {
                continue;
            }
            double sim = cosine(query, vec);
            scores.add(new long[]{Double.doubleToLongBits(sim), i});
        }
        scores.sort(Comparator.<long[]>comparingLong(s -> s[0]).reversed());
        if (scores.size() > topK) {
            scores = scores.subList(0, topK);
        }

        Map<Long, CodeChunk> chunkById = new HashMap<>();
        List<SearchHit> hits = new ArrayList<>();
        for (long[] entry : scores) {
            int idx = (int) entry[1];
            double sim = Double.longBitsToDouble(entry[0]);
            CodeEmbedding emb = candidates.get(idx);
            CodeChunk chunk = chunkById.computeIfAbsent(emb.getChunkId(), id ->
                    chunkRepository.findById(id).orElse(null));
            if (chunk == null) {
                continue;
            }
            hits.add(new SearchHit(chunk.getId(), sim, chunk.getFilePath(), chunk.getClassName(),
                    chunk.getMethodName(), chunk.getChunkType(), chunk.getStartLine(), chunk.getEndLine(),
                    chunk.getContent()));
        }
        return hits;
    }

    private float[] toVector(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            List<Double> values = objectMapper.readValue(json, new TypeReference<List<Double>>() {
            });
            float[] v = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                v[i] = values.get(i).floatValue();
            }
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    private double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    @Override
    public void cleanUpProjectExcept(Long projectId, Long repositoryId) {
        // keeps future tenants isolated; not needed for current deployments
    }
}