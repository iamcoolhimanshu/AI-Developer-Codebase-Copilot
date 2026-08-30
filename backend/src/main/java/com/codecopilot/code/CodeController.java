package com.codecopilot.code;

import com.codecopilot.code.entity.CodeClass;
import com.codecopilot.code.entity.CodeDependency;
import com.codecopilot.code.entity.CodeField;
import com.codecopilot.code.entity.CodeMethod;
import com.codecopilot.code.entity.RepositoryFile;
import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.code.CodeExplorerService.ArchitectureGraph;
import com.codecopilot.code.CodeExplorerService.WhereUsedResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CodeController {

	private final CodeExplorerService explorer;

	public CodeController(CodeExplorerService explorer) {
		this.explorer = explorer;
	}

	@GetMapping("/projects/{projectId}/files")
	public ApiResponse<List<RepositoryFile>> files(@PathVariable Long projectId,
			@RequestParam(required = false) Long repositoryId) {
		return ApiResponse.ok(explorer.files(projectId, repositoryId));
	}

	@GetMapping("/projects/{projectId}/files/{fileId}")
	public ApiResponse<RepositoryFile> file(@PathVariable Long projectId, @PathVariable Long fileId) {
		return ApiResponse.ok(explorer.fileContent(projectId, fileId));
	}

	@GetMapping("/projects/{projectId}/classes")
	public ApiResponse<List<CodeClass>> classes(@PathVariable Long projectId,
			@RequestParam(required = false) Long repositoryId, @RequestParam(required = false) String query) {
		if (query == null || query.isBlank()) {
			return ApiResponse.ok(explorer.classes(projectId, repositoryId));
		}
		return ApiResponse.ok(explorer.searchClasses(projectId, query));
	}

	@GetMapping("/projects/{projectId}/classes/{classId}")
	public ApiResponse<CodeClass> classDetail(@PathVariable Long projectId, @PathVariable Long classId) {
		return ApiResponse.ok(explorer.classDetail(projectId, classId));
	}

	@GetMapping("/projects/{projectId}/classes/{classId}/methods")
	public ApiResponse<List<CodeMethod>> methods(@PathVariable Long projectId, @PathVariable Long classId) {
		return ApiResponse.ok(explorer.methodsOf(projectId, classId));
	}

	@GetMapping("/projects/{projectId}/classes/{classId}/fields")
	public ApiResponse<List<CodeField>> fields(@PathVariable Long projectId, @PathVariable Long classId) {
		return ApiResponse.ok(explorer.fieldsOf(projectId, classId));
	}

	@GetMapping("/projects/{projectId}/api-endpoints")
	public ApiResponse<List<CodeMethod>> endpoints(@PathVariable Long projectId,
			@RequestParam(required = false) Long repositoryId) {
		return ApiResponse.ok(explorer.apiEndpoints(projectId, repositoryId));
	}

	@GetMapping("/projects/{projectId}/architecture")
	public ApiResponse<ArchitectureGraph> architecture(@PathVariable Long projectId,
			@RequestParam(required = false) Long repositoryId) {
		return ApiResponse.ok(explorer.architecture(projectId, repositoryId));
	}

	@GetMapping("/projects/{projectId}/dependencies")
	public ApiResponse<List<CodeDependency>> dependencies(@PathVariable Long projectId,
			@RequestParam(required = false) Long repositoryId) {
		return ApiResponse.ok(explorer.dependencies(projectId, repositoryId));
	}

	@GetMapping("/projects/{projectId}/where-used")
	public ApiResponse<List<WhereUsedResult>> whereUsed(@PathVariable Long projectId, @RequestParam String symbol,
			@RequestParam(required = false) String type) {
		return ApiResponse.ok(explorer.whereUsed(projectId, symbol, type));
	}
}