package com.codecopilot.ai.tools;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolExecutionRepository extends JpaRepository<com.codecopilot.ai.tools.entity.ToolExecution, Long> {

    List<com.codecopilot.ai.tools.entity.ToolExecution> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<com.codecopilot.ai.tools.entity.ToolExecution> findByUserIdOrderByCreatedAtDesc(Long userId);
}