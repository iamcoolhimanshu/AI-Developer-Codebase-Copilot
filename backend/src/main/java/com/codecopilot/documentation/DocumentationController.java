package com.codecopilot.documentation;

import com.codecopilot.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class DocumentationController {

	private final DocumentationService documentationService;

	public DocumentationController(DocumentationService documentationService) {
		this.documentationService = documentationService;
	}

	@PostMapping("/documentation")
	public ApiResponse<DocumentationService.GeneratedDocument> generate(@PathVariable Long projectId,
			@RequestParam Long repositoryId, @RequestParam(defaultValue = "README") String type) {
		return ApiResponse.ok(documentationService.generate(projectId, repositoryId, type));
	}
}