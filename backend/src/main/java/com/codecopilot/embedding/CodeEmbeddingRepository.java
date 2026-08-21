package com.codecopilot.embedding;

import com.codecopilot.embedding.entity.CodeEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeEmbeddingRepository extends JpaRepository<CodeEmbedding, Long> {

    List<CodeEmbedding> findByProjectIdAndRepositoryId(Long projectId, Long repositoryId);

    List<CodeEmbedding> findByProjectIdAndFileIdIn(Long projectId, List<Long> fileIds);

    void deleteByRepositoryId(Long repositoryId);

    void deleteByChunkId(Long chunkId);

    void deleteByFileId(Long fileId);
}