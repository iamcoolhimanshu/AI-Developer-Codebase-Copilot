package com.codecopilot.code.repo;

import com.codecopilot.code.entity.CodeDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeDependencyRepository extends JpaRepository<CodeDependency, Long> {

    List<CodeDependency> findByProjectIdAndRepositoryId(Long projectId, Long repositoryId);

    List<CodeDependency> findByProjectIdAndSourceClassFq(Long projectId, String sourceClassFq);

    List<CodeDependency> findByProjectIdAndTargetClassFq(Long projectId, String targetClassFq);

    List<CodeDependency> findByProjectId(Long projectId);

    long countByProjectId(Long projectId);

    void deleteByRepositoryId(Long repositoryId);

    void deleteByRepositoryIdAndFileId(Long repositoryId, Long fileId);
}