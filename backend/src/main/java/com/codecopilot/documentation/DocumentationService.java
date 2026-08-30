package com.codecopilot.documentation;

import com.codecopilot.ai.AiService;
import com.codecopilot.ai.prompts.Prompts;
import com.codecopilot.code.entity.CodeClass;
import com.codecopilot.code.entity.CodeDependency;
import com.codecopilot.code.entity.CodeMethod;
import com.codecopilot.code.repo.CodeClassRepository;
import com.codecopilot.code.repo.CodeDependencyRepository;
import com.codecopilot.code.repo.CodeMethodRepository;
import com.codecopilot.project.ProjectAccessService;
import com.codecopilot.project.Project;
import com.codecopilot.project.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentationService {

	private final AiService aiService;
	private final ProjectAccessService accessService;
	private final ProjectRepository projectRepository;
	private final CodeClassRepository classRepository;
	private final CodeMethodRepository methodRepository;
	private final CodeDependencyRepository dependencyRepository;

	public DocumentationService(AiService aiService, ProjectAccessService accessService,
			ProjectRepository projectRepository, CodeClassRepository classRepository,
			CodeMethodRepository methodRepository, CodeDependencyRepository dependencyRepository) {
		this.aiService = aiService;
		this.accessService = accessService;
		this.projectRepository = projectRepository;
		this.classRepository = classRepository;
		this.methodRepository = methodRepository;
		this.dependencyRepository = dependencyRepository;
	}

	public GeneratedDocument generate(Long projectId, Long repositoryId, String docType) {
		accessService.requireView(projectId);
		String projectName = projectRepository.findById(projectId).map(Project::getName).orElse("?");
		Project project = projectRepository.findById(projectId).orElse(null);

		String corpus = buildCorpus(projectId, repositoryId, project);
		String system = Prompts.documentationSystem(projectName, docType);
		String userPrompt = "Write the " + docType + " documentation using this repository evidence:\n\n" + corpus;
		String doc = aiService.chat(system, userPrompt);

		return new GeneratedDocument(docType, doc, aiService.isConfigured() ? "grok" : null);
	}

	private String buildCorpus(Long projectId, Long repositoryId, Project project) {
		StringBuilder sb = new StringBuilder();
		if (project != null) {
			sb.append("Project: ").append(project.getName()).append("\n");
			sb.append("Description: ").append(project.getDescription()).append("\n");
			sb.append("Technologies: ").append(String.join(", ", project.getTechnologies())).append("\n\n");
		}
		List<CodeClass> classes = classRepository.findByProjectIdAndRepositoryId(projectId, repositoryId);
		sb.append("Packages / classes:\n");
		for (CodeClass c : classes) {
			sb.append("- ").append(c.getFqName()).append(" (").append(c.getKind()).append(") ")
					.append(describe(stereotype(c))).append("\n");
		}
		sb.append("\nAPI endpoints:\n");
		List<CodeMethod> endpoints = methodRepository.findByProjectIdAndRepositoryIdAndHttpPathNotNull(projectId,
				repositoryId);
		for (CodeMethod m : endpoints) {
			sb.append("- ").append(m.getHttpMethod()).append(' ').append(m.getHttpPath()).append(" -> ")
					.append(canonicalClass(classes, m)).append('.').append(m.getName()).append("\n");
		}
		sb.append("\nDependencies:\n");
		List<CodeDependency> deps = dependencyRepository.findByProjectIdAndRepositoryId(projectId, repositoryId);
		for (CodeDependency d : deps) {
			sb.append("- ").append(d.getSourceClassFq()).append(" --").append(d.getType()).append("--> ")
					.append(d.getTargetClassFq()).append("\n");
		}
		return sb.toString();
	}

	private String canonicalClass(List<CodeClass> classes, CodeMethod m) {
		return classes.stream().filter(c -> c.getId().equals(m.getClassId())).map(CodeClass::getName).findFirst()
				.orElse("?");
	}

	private String stereotype(CodeClass c) {
		String anns = c.getAnnotations() == null ? "" : c.getAnnotations();
		if (anns.contains("RestController") || anns.contains("Controller"))
			return "controller";
		if (anns.contains("Service"))
			return "service";
		if (anns.contains("Repository"))
			return "repository";
		if (anns.contains("Entity"))
			return "entity";
		if (anns.contains("Configuration"))
			return "configuration";
		if ("INTERFACE".equals(c.getKind()))
			return "interface";
		return "model";
	}

	private String describe(String s) {
		return "[" + s + "]";
	}

	public record GeneratedDocument(String type, String content, String model) {
	}
}