package com.codecopilot.code.repo;

import com.codecopilot.code.entity.RepositoryFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositoryFileRepository extends JpaRepository<RepositoryFile, Long> {

	List<RepositoryFile> findByProjectIdAndRepositoryIdOrderByPath(Long projectId, Long repositoryId);

	Optional<RepositoryFile> findByProjectIdAndRepositoryIdAndPath(Long projectId, Long repositoryId, String path);

	void deleteByRepositoryId(Long repositoryId);

	long countByProjectIdAndRepositoryId(Long projectId, Long repositoryId);

	long countByProjectId(Long projectId);

	List<RepositoryFile> findByProjectIdAndRepositoryIdAndPathStartingWith(Long projectId, Long repositoryId,
			String prefix);

	void deleteByRepositoryIdAndPathNotIn(Long repositoryId, List<String> paths);
}