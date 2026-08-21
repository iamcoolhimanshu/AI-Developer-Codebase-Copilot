package com.codecopilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GitRepositoryRepository extends JpaRepository<GitRepository, Long> {

    List<GitRepository> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<GitRepository> findByIdAndProjectId(Long id, Long projectId);
}