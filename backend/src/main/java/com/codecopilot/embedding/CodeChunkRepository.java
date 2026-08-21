package com.codecopilot.embedding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeChunkRepository extends JpaRepository<com.codecopilot.embedding.entity.CodeChunk, Long> {

    List<com.codecopilot.embedding.entity.CodeChunk> findByProjectIdAndRepositoryId(Long projectId, Long repositoryId);

    List<com.codecopilot.embedding.entity.CodeChunk> findByProjectIdAndRepositoryIdAndChecksumIn(
            Long projectId, Long repositoryId, List<String> checksums);

    void deleteByRepositoryId(Long repositoryId);

    void deleteByFileId(Long fileId);

    long countByProjectId(Long projectId);

    long countByProjectIdAndRepositoryId(Long projectId, Long repositoryId);
}