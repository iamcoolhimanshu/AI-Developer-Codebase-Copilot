package com.codecopilot.analysis.repo;

import com.codecopilot.analysis.entity.AnalysisRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, Long> {

    List<AnalysisRun> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}