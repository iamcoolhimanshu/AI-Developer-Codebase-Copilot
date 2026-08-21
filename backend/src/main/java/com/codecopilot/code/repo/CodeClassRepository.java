package com.codecopilot.code.repo;

import com.codecopilot.code.entity.CodeClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeClassRepository extends JpaRepository<CodeClass, Long> {

    List<CodeClass> findByProjectIdAndRepositoryId(Long projectId, Long repositoryId);

    List<CodeClass> findByProjectIdAndRepositoryIdAndFqName(Long projectId, Long repositoryId, String fqName);

    List<CodeClass> findByProjectIdAndRepositoryIdAndName(Long projectId, Long repositoryId, String name);

    Page<CodeClass> findByProjectId(Long pid, Pageable pageable);

    long countByProjectId(Long projectId);

    long countByProjectIdAndRepositoryId(Long projectId, Long repositoryId);

    void deleteByRepositoryId(Long repositoryId);

    void deleteByRepositoryIdAndFileId(Long repositoryId, Long fileId);

    List<CodeClass> findByProjectIdAndRepositoryIdAndAnnotationsLike(
            Long projectId, Long repositoryId, String annotationPattern);
}