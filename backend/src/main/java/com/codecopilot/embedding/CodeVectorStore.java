package com.codecopilot.embedding;

import com.codecopilot.embedding.entity.CodeEmbedding;

import java.util.List;

/**
 * Abstraction over vector persistence so the MySQL JSON-based implementation
 * can be swapped for a native vector database later without impacting callers.
 */
public interface CodeVectorStore {

    void upsert(List<CodeEmbedding> embeddings);

    void deleteByRepository(Long repositoryId);

    List<JpaCodeVectorStore.SearchHit> search(float[] query, Long projectId, List<Long> repositoryIds, int topK);

    void cleanUpProjectExcept(Long projectId, Long repositoryId);
}