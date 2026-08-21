package com.codecopilot.observability;

import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.common.security.SecurityUtils;
import com.codecopilot.user.RoleName;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final MetricsService metricsService;

    public AdminController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/metrics")
    public ApiResponse<Map<String, Object>> metrics() {
        if (!SecurityUtils.hasRole(RoleName.ADMIN.name())) {
            throw new com.codecopilot.common.exception.ForbiddenException("Admin only");
        }
        return ApiResponse.ok(metricsService.snapshot());
    }
}