package com.codecopilot.analysis;

import com.codecopilot.analysis.dto.BugAnalysisRequest;
import com.codecopilot.analysis.dto.BugAnalysisResponse;
import com.codecopilot.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class AnalysisController {

    private final BugInvestigatorService investigator;

    public AnalysisController(BugInvestigatorService investigator) {
        this.investigator = investigator;
    }

    @PostMapping("/bug-analysis")
    public ApiResponse<BugAnalysisResponse> investigate(@PathVariable Long projectId,
                                                        @Valid @RequestBody BugAnalysisRequest request) {
        return ApiResponse.ok(investigator.investigate(projectId, request));
    }
}