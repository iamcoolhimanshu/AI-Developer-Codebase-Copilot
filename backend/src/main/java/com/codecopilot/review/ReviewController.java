package com.codecopilot.review;

import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.review.dto.ReviewRequest;
import com.codecopilot.review.dto.ReviewResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ReviewController {

    private final CodeReviewService reviewService;

    public ReviewController(CodeReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/code-review")
    public ApiResponse<ReviewResponse> review(@PathVariable Long projectId,
                                              @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(reviewService.review(projectId, request));
    }
}