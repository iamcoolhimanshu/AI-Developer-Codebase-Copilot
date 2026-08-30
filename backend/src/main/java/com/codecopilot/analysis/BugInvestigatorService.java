package com.codecopilot.analysis;

import com.codecopilot.ai.AiService;
import com.codecopilot.ai.prompts.Prompts;
import com.codecopilot.analysis.dto.BugAnalysisRequest;
import com.codecopilot.analysis.dto.BugAnalysisResponse;
import com.codecopilot.analysis.entity.AnalysisRun;
import com.codecopilot.analysis.entity.AnalysisFinding;
import com.codecopilot.analysis.repo.AnalysisFindingRepository;
import com.codecopilot.analysis.repo.AnalysisRunRepository;
import com.codecopilot.code.entity.CodeClass;
import com.codecopilot.code.entity.CodeMethod;
import com.codecopilot.code.entity.RepositoryFile;
import com.codecopilot.code.repo.CodeClassRepository;
import com.codecopilot.code.repo.CodeMethodRepository;
import com.codecopilot.code.repo.CodeReferenceRepository;
import com.codecopilot.code.repo.RepositoryFileRepository;
import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.git.GitIntelligenceService;
import com.codecopilot.git.GitIntelligenceService.CommitInfo;
import com.codecopilot.project.ProjectAccessService;
import com.codecopilot.project.ProjectRepository;
import com.codecopilot.project.Project;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BugInvestigatorService {

	private static final Pattern FILE_LINE = Pattern.compile("([\\w$]+\\.java):(\\d+)");

	private final RepositoryFileRepository fileRepository;
	private final CodeClassRepository classRepository;
	private final CodeMethodRepository methodRepository;
	private final CodeReferenceRepository referenceRepository;
	private final GitIntelligenceService gitService;
	private final AiService aiService;
	private final ProjectAccessService accessService;
	private final ProjectRepository projectRepository;
	private final AnalysisRunRepository runRepository;
	private final AnalysisFindingRepository findingRepository;

	public BugInvestigatorService(RepositoryFileRepository fileRepository, CodeClassRepository classRepository,
			CodeMethodRepository methodRepository, CodeReferenceRepository referenceRepository,
			GitIntelligenceService gitService, AiService aiService, ProjectAccessService accessService,
			ProjectRepository projectRepository, AnalysisRunRepository runRepository,
			AnalysisFindingRepository findingRepository) {
		this.fileRepository = fileRepository;
		this.classRepository = classRepository;
		this.methodRepository = methodRepository;
		this.referenceRepository = referenceRepository;
		this.gitService = gitService;
		this.aiService = aiService;
		this.accessService = accessService;
		this.projectRepository = projectRepository;
		this.runRepository = runRepository;
		this.findingRepository = findingRepository;
	}

	public BugAnalysisResponse investigate(Long projectId, BugAnalysisRequest request) {
		accessService.requireView(projectId);
		List<RepositoryFile> files = filesOfProject(projectId, request.getRepositoryIds());

		Map<String, Object> evidenceBundle = new LinkedHashMap<>();
		evidenceBundle.put("error", request.getErrorMessage());
		if (request.getStackTrace() != null && !request.getStackTrace().isBlank()) {
			evidenceBundle.put("stackTrace", request.getStackTrace());
		}

		String errorClass = firstErrorClass(request);
		evidenceBundle.put("errorClass", errorClass);

		// 1. Locate stack files
		List<LocatedFile> located = new ArrayList<>();
		if (request.getFilePath() != null && !request.getFilePath().isBlank()) {
			RepositoryFile rf = files.stream().filter(f -> f.getPath().endsWith(request.getFilePath())).findFirst()
					.orElse(null);
			if (rf != null) {
				located.add(new LocatedFile(rf, request.getLineNumber()));
			}
		}
		if (located.isEmpty() && request.getStackTrace() != null) {
			Matcher m = FILE_LINE.matcher(request.getStackTrace());
			while (m.find()) {
				String fname = m.group(1);
				int line = Integer.parseInt(m.group(2));
				RepositoryFile rf = files.stream().filter(f -> f.getFileName().equals(fname)).findFirst().orElse(null);
				if (rf != null) {
					located.add(new LocatedFile(rf, line));
					break;
				}
			}
		}

		List<BugAnalysisResponse.Evidence> evidence = new ArrayList<>();
		for (LocatedFile lf : located) {
			evidence.addAll(buildFileEvidence(projectId, lf, files, request.getRepositoryIds()));
		}

		// 2. Recent Git changes for targeted files
		List<CommitInfo> recentCommits = new ArrayList<>();
		for (LocatedFile lf : located) {
			List<Long> repoIds = request.getRepositoryIds() == null || request.getRepositoryIds().isEmpty() ? List.of()
					: request.getRepositoryIds();
			String relPath = relativeToRepo(lf.file(), projectId, repoIds);
			if (relPath != null) {
				recentCommits.addAll(gitService.commits(projectId, repoIdOf(lf.file(), repoIds), relPath, 5));
			}
		}
		if (!recentCommits.isEmpty()) {
			evidence.add(new BugAnalysisResponse.Evidence("git history", null, null, null, null,
					String.join("\n",
							recentCommits.stream().limit(8).map(c -> c.shortId() + " " + c.message()).toList()),
					"GIT"));
		}

		// 3. Reasoning prompt
		String projectName = projectRepository.findById(projectId).map(Project::getName).orElse("?");
		StringBuilder context = new StringBuilder();
		int idx = 1;
		for (BugAnalysisResponse.Evidence e : evidence) {
			context.append(String.format("[%d] %s%s%n%s%n%n", idx++, e.getFilePath(),
					e.getClassName() == null ? "" : " (" + e.getClassName() + "." + e.getMethodName() + ")",
					e.getSnippet() == null ? "" : e.getSnippet()));
		}
		String system = Prompts.bugInvestigationSystem(projectName, request.getErrorMessage());
		String userPrompt = "Investigate the error using this evidence:\n\n" + context;

		String analysis = aiService.chat(system, userPrompt);
		String confidence = extractConfidence(analysis);

		// Persist run
		AnalysisRun run = new AnalysisRun();
		run.setProjectId(projectId);
		run.setType("BUG_ANALYSIS");
		run.setStatus("COMPLETED");
		run.setInput(request.getErrorMessage());
		run.setResult(analysis);
		run = runRepository.save(run);

		for (BugAnalysisResponse.Evidence e : evidence) {
			AnalysisFinding f = new AnalysisFinding();
			f.setRunId(run.getId());
			f.setProjectId(projectId);
			f.setType(e.getType());
			f.setFilePath(e.getFilePath());
			f.setDetail(e.getClassName() + (e.getMethodName() == null ? "" : "." + e.getMethodName()));
			findingRepository.save(f);
		}

		return BugAnalysisResponse.builder().analysis(analysis).confidence(confidence)
				.likelyRootCause(extractRootCause(analysis)).evidence(evidence).errorClass(errorClass).build();
	}

	private List<BugAnalysisResponse.Evidence> buildFileEvidence(Long projectId, LocatedFile lf,
			List<RepositoryFile> allFiles, List<Long> repoIds) {
		List<BugAnalysisResponse.Evidence> out = new ArrayList<>();
		String content = lf.file().getContent();
		String[] lines = content == null ? new String[0] : content.split("\n", -1);
		int line = lf.line() == null ? 1 : lf.line();
		int start = Math.max(0, line - 6);
		int end = Math.min(lines.length, line + 6);
		StringBuilder window = new StringBuilder();
		for (int i = start; i < end; i++) {
			window.append(String.format("%5d| %s%n", i + 1, lines[i]));
		}
		out.add(new BugAnalysisResponse.Evidence(lf.file().getPath(), null, null, Math.max(1, line - 6), end,
				window.toString(), "SOURCE"));

		// Enclosing method
		CodeClass cls = classesOfFile(projectId, lf.file().getId()).stream().findFirst().orElse(null);
		if (cls != null) {
			List<CodeMethod> methods = methodRepository.findByProjectIdAndRepositoryIdAndClassId(projectId,
					cls.getRepositoryId(), cls.getId());
			CodeMethod enclosing = methods.stream().filter(m -> line >= m.getStartLine() && line <= m.getEndLine())
					.findFirst().orElse(null);
			if (enclosing != null) {
				out.add(new BugAnalysisResponse.Evidence(lf.file().getPath(), cls.getName(), enclosing.getName(),
						enclosing.getStartLine(), enclosing.getEndLine(), "```java\n" + enclosing.getBody() + "\n```",
						"METHOD"));

				// 3. Callers
				for (var ref : referenceRepository.findByProjectIdAndTargetName(projectId, enclosing.getName())) {
					if (!ref.getSourceClassFq().equals(cls.getFqName())) {
						out.add(new BugAnalysisResponse.Evidence(filePathOf(ref.getFileId()), ref.getSourceClassFq(),
								ref.getSourceMethodName(), null, null, ref.getType(), "CALLER"));
					}
				}
			}
		}
		return out;
	}

	private List<CodeClass> classesOfFile(Long projectId, Long fileId) {
		return classRepository.findAll().stream()
				.filter(c -> c.getProjectId().equals(projectId) && c.getFileId().equals(fileId)).toList();
	}

	private List<RepositoryFile> filesOfProject(Long projectId, List<Long> repoIds) {
		if (repoIds == null || repoIds.isEmpty()) {
			return fileRepository.findAll().stream().filter(f -> f.getProjectId().equals(projectId)).toList();
		}
		List<RepositoryFile> out = new ArrayList<>();
		for (Long rid : repoIds) {
			out.addAll(fileRepository.findByProjectIdAndRepositoryIdOrderByPath(projectId, rid));
		}
		return out;
	}

	private String firstErrorClass(BugAnalysisRequest request) {
		String s = request.getErrorMessage() == null ? "" : request.getErrorMessage();
		Matcher m = Pattern.compile("[\\w$.]+Exception").matcher(s);
		return m.find() ? m.group() : null;
	}

	private String extractConfidence(String analysis) {
		Matcher m = Pattern.compile("(?i)confidence[:\\s]*(high|medium|low)").matcher(analysis);
		return m.find() ? m.group(1).toUpperCase() : "UNKNOWN";
	}

	private String extractRootCause(String analysis) {
		int idx = analysis.toLowerCase().indexOf("root cause");
		if (idx < 0) {
			idx = analysis.toLowerCase().indexOf("likely cause");
		}
		if (idx < 0) {
			return null;
		}
		String segment = analysis.substring(idx);
		return segment.split("\n")[0].replaceAll("^[*#\\s]+", "").trim();
	}

	private String filePathOf(Long fileId) {
		if (fileId == null)
			return "";
		return fileRepository.findById(fileId).map(RepositoryFile::getPath).orElse("");
	}

	private Long repoIdOf(RepositoryFile file, List<Long> repoIds) {
		return file.getRepositoryId();
	}

	private String relativeToRepo(RepositoryFile file, Long projectId, List<Long> repoIds) {
		return file.getPath();
	}

	private record LocatedFile(RepositoryFile file, Integer line) {
	}
}