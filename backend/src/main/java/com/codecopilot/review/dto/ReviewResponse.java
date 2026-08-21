package com.codecopilot.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private String summary;
    private List<Finding> findings;
    private String raw;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Finding {
        private String severity;
        private String category;
        private String confidence;
        private String location;
        private String title;
        private String detail;
        private String suggestion;
    }
}