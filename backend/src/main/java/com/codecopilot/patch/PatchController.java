package com.codecopilot.patch;

import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.patch.dto.PatchRequest;
import com.codecopilot.patch.entity.GeneratedPatch;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/patches")
public class PatchController {

    private final PatchService patchService;

    public PatchController(PatchService patchService) {
        this.patchService = patchService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GeneratedPatch>> generate(@PathVariable Long projectId,
                                                                @Valid @RequestBody PatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Patch generated - review before applying", patchService.generate(projectId, request)));
    }

    @GetMapping
    public ApiResponse<List<GeneratedPatch>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(patchService.list(projectId));
    }

    @PostMapping("/{patchId}/approve")
    public ApiResponse<GeneratedPatch> approve(@PathVariable Long projectId, @PathVariable Long patchId) {
        return ApiResponse.ok(patchService.approve(projectId, patchId));
    }

    @PostMapping("/{patchId}/apply")
    public ApiResponse<GeneratedPatch> apply(@PathVariable Long projectId, @PathVariable Long patchId) {
        return ApiResponse.ok("Patch applied", patchService.apply(projectId, patchId));
    }

    @PostMapping("/{patchId}/reject")
    public ApiResponse<GeneratedPatch> reject(@PathVariable Long projectId, @PathVariable Long patchId) {
        return ApiResponse.ok(patchService.reject(projectId, patchId));
    }
}