package com.codecopilot.embedding;

import com.codecopilot.embedding.entity.CodeEmbedding;

import java.util.List;

public interface CodeVectorStore {

	void upsert(List<CodeEmbedding> embeddings);

	void deleteByRepository(Long repositoryId);

	List<JpaCodeVectorStore.SearchHit> search(float[] query, Long projectId, List<Long> repositoryIds, int topK);

	void cleanUpProjectExcept(Long projectId, Long repositoryId);
}