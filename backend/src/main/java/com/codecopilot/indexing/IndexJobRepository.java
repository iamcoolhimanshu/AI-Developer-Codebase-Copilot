package com.codecopilot.indexing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndexJobRepository extends JpaRepository<IndexJob, Long> {

    Optional<IndexJob> findTopByRepositoryIdOrderByCreatedAtDesc(Long repositoryId);

    List<IndexJob> findByRepositoryIdOrderByCreatedAtDesc(Long repositoryId);

    boolean existsByRepositoryIdAndStatusIn(Long repositoryId, List<IndexJob.IndexStatus> statuses);
}