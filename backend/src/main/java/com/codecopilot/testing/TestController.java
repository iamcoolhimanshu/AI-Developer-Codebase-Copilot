package com.codecopilot.testing;

import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.testing.dto.TestGenerationRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class TestController {

    private final TestGeneratorService testGeneratorService;

    public TestController(TestGeneratorService testGeneratorService) {
        this.testGeneratorService = testGeneratorService;
    }

    @PostMapping("/test-generation")
    public ApiResponse<TestGeneratorService.GeneratedTests> generate(@PathVariable Long projectId,
                                                                     @Valid @RequestBody TestGenerationRequest request) {
        return ApiResponse.ok(testGeneratorService.generate(projectId, request.getClassId(), request.getMethodName()));
    }
}