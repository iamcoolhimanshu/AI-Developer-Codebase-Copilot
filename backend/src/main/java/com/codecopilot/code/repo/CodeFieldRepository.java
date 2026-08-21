package com.codecopilot.code.repo;

import com.codecopilot.code.entity.CodeField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeFieldRepository extends JpaRepository<CodeField, Long> {

    List<CodeField> findByProjectIdAndRepositoryId(Long projectId, Long repositoryId);

    List<CodeField> findByProjectIdAndRepositoryIdAndClassId(Long projectId, Long repositoryId, Long classId);

    void deleteByRepositoryId(Long repositoryId);

    void deleteByRepositoryIdAndFileId(Long repositoryId, Long fileId);
}