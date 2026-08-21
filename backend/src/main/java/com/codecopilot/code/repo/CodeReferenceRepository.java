package com.codecopilot.code.repo;

import com.codecopilot.code.entity.CodeReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeReferenceRepository extends JpaRepository<CodeReference, Long> {

    List<CodeReference> findByProjectIdAndRepositoryId(Long projectId, Long repositoryId);

    List<CodeReference> findByProjectIdAndTargetName(Long projectId, String targetName);

    List<CodeReference> findByProjectIdAndTargetNameAndTargetClassFq(
            Long projectId, String targetName, String targetClassFq);

    List<CodeReference> findByProjectIdAndRepositoryIdAndTargetName(
            Long projectId, Long repositoryId, String targetName);

    List<CodeReference> findByProjectIdAndSourceClassFq(Long projectId, String sourceClassFq);

    void deleteByRepositoryId(Long repositoryId);

    void deleteByRepositoryIdAndFileId(Long repositoryId, Long fileId);
}