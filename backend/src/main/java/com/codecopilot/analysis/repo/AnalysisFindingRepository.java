package com.codecopilot.analysis.repo;

import com.codecopilot.analysis.entity.AnalysisFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisFindingRepository extends JpaRepository<AnalysisFinding, Long> {

    List<AnalysisFinding> findByRunId(Long runId);

    void deleteByRunId(Long runId);
}