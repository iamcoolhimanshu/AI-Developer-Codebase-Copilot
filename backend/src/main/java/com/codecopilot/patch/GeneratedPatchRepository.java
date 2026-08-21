package com.codecopilot.patch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeneratedPatchRepository extends JpaRepository<com.codecopilot.patch.entity.GeneratedPatch, Long> {

    List<com.codecopilot.patch.entity.GeneratedPatch> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<com.codecopilot.patch.entity.GeneratedPatch> findByUserIdOrderByCreatedAtDesc(Long userId);
}