package com.codecopilot.code.repo;

import com.codecopilot.code.entity.CodeMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeMethodRepository extends JpaRepository<CodeMethod, Long> {

	List<CodeMethod> findByProjectIdAndRepositoryId(Long projectId, Long repositoryId);

	List<CodeMethod> findByProjectIdAndRepositoryIdAndClassId(Long projectId, Long repositoryId, Long classId);

	Optional<CodeMethod> findByProjectIdAndRepositoryIdAndClassIdAndName(Long projectId, Long repositoryId,
			Long classId, String name);

	long countByProjectId(Long projectId);

	long countByProjectIdAndRepositoryId(Long projectId, Long repositoryId);

	long countByProjectIdAndHttpPathNotNull(Long projectId);

	List<CodeMethod> findByProjectIdAndHttpPathNotNull(Long projectId);

	List<CodeMethod> findByProjectIdAndRepositoryIdAndHttpPathNotNull(Long projectId, Long repositoryId);

	List<CodeMethod> findByProjectIdAndHttpPathContaining(Long projectId, String pathFragment);

	void deleteByRepositoryId(Long repositoryId);

	void deleteByRepositoryIdAndFileId(Long repositoryId, Long fileId);
}