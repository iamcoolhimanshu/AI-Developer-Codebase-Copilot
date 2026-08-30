package com.codecopilot.code.repo;

import com.codecopilot.code.entity.CodePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodePackageRepository extends JpaRepository<CodePackage, Long> {

	List<CodePackage> findByProjectIdAndRepositoryId(Long projectId, Long repositoryId);

	Optional<CodePackage> findByProjectIdAndRepositoryIdAndName(Long projectId, Long repositoryId, String name);

	void deleteByRepositoryId(Long repositoryId);
}