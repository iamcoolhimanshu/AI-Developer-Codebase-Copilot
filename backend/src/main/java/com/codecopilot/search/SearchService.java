package com.codecopilot.search;

import com.codecopilot.code.entity.CodeClass;
import com.codecopilot.code.entity.CodeMethod;
import com.codecopilot.code.entity.RepositoryFile;
import com.codecopilot.code.repo.CodeClassRepository;
import com.codecopilot.code.repo.CodeMethodRepository;
import com.codecopilot.code.repo.RepositoryFileRepository;
import com.codecopilot.embedding.CodeVectorStore;
import com.codecopilot.embedding.EmbeddingService;
import com.codecopilot.embedding.JpaCodeVectorStore;
import com.codecopilot.search.dto.SearchRequest;
import com.codecopilot.search.dto.SearchResultDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SearchService {

	private final CodeClassRepository classRepository;
	private final CodeMethodRepository methodRepository;
	private final RepositoryFileRepository fileRepository;
	private final CodeVectorStore vectorStore;
	private final EmbeddingService embeddingService;

	public SearchService(CodeClassRepository classRepository, CodeMethodRepository methodRepository,
			RepositoryFileRepository fileRepository, CodeVectorStore vectorStore, EmbeddingService embeddingService) {
		this.classRepository = classRepository;
		this.methodRepository = methodRepository;
		this.fileRepository = fileRepository;
		this.vectorStore = vectorStore;
		this.embeddingService = embeddingService;
	}

	public List<SearchResultDto> search(Long projectId, SearchRequest request) {
		String query = request.getQuery().trim();
		int topK = request.getTopK() == null ? 20 : Math.min(50, request.getTopK());
		List<Long> repoIds = request.getRepositoryIds();

		Map<String, SearchResultDto> merged = new LinkedHashMap<>();

		// 1. Exact/starts-with symbol hits
		exactClassHits(projectId, repoIds, query).forEach(r -> put(merged, r, 1.5));

		// 2. Substring keyword hits on files/classes/methods
		substringHits(projectId, repoIds, query).forEach(r -> put(merged, r, 1.0));

		// 3. Structured filter hits (language / kind)
		if (request.getLanguage() != null || request.getTypes() != null || repoIds != null) {
			filteredHits(projectId, repoIds, request).forEach(r -> put(merged, r, 0.8));
		}

		// 4. Semantic vector hits
		if (embeddingService.aiEnabled() || true) {
			float[] vec = embeddingService.embed(query);
			List<JpaCodeVectorStore.SearchHit> hits = vectorStore.search(vec, projectId,
					repoIds == null || repoIds.isEmpty() ? null : List.of(), topK);
			hits.forEach(h -> put(merged, toResult(h), 0.9));
		}

		return merged.values().stream().sorted((a, b) -> Double.compare(b.getScore(), a.getScore())).limit(topK * 2)
				.toList();
	}

	private void put(Map<String, SearchResultDto> map, SearchResultDto dto, double weight) {
		String key = dto.getFilePath() + ":" + dto.getClassName() + ":" + dto.getMethodName() + ":"
				+ dto.getMatchType();
		SearchResultDto existing = map.get(key);
		if (existing == null) {
			dto.setScore(dto.getScore() * weight);
			map.put(key, dto);
		} else {
			existing.setScore(existing.getScore() + dto.getScore() * weight);
		}
	}

	private List<SearchResultDto> exactClassHits(Long projectId, List<Long> repoIds, String query) {
		List<SearchResultDto> out = new ArrayList<>();
		List<CodeClass> classes;
		if (repoIds == null || repoIds.isEmpty()) {
			classes = classRepository.findAll().stream().filter(c -> c.getProjectId().equals(projectId)).toList();
		} else {
			classes = new ArrayList<>();
			for (Long rid : repoIds) {
				classes.addAll(classRepository.findByProjectIdAndRepositoryId(projectId, rid));
			}
		}
		String q = query.toLowerCase(Locale.ROOT);
		for (CodeClass c : classes) {
			boolean exactName = c.getName().equalsIgnoreCase(query);
			boolean contains = c.getName().toLowerCase(Locale.ROOT).contains(q);
			if (exactName) {
				out.add(SearchResultDto.builder().matchType("SYMBOL").filePath(pathOf(c)).className(c.getName())
						.kind("class").startLine(c.getStartLine()).endLine(c.getEndLine()).sourceType(c.getKind())
						.score(1.0).repositoryId(c.getRepositoryId()).build());
			} else if (contains && q.length() > 2) {
				out.add(SearchResultDto.builder().matchType("KEYWORD").filePath(pathOf(c)).className(c.getName())
						.kind("class").startLine(c.getStartLine()).endLine(c.getEndLine()).sourceType(c.getKind())
						.score(0.6).repositoryId(c.getRepositoryId()).build());
			}
		}
		return out;
	}

	private String pathOf(CodeClass c) {

		return fileRepository.findById(c.getFileId()).map(RepositoryFile::getPath).orElse("");
	}

	private List<SearchResultDto> substringHits(Long projectId, List<Long> repoIds, String query) {
		String q = query.toLowerCase(Locale.ROOT);
		List<SearchResultDto> out = new ArrayList<>();
		for (RepositoryFile f : filesFor(projectId, repoIds)) {
			if (f.getPath().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
				out.add(SearchResultDto.builder().matchType("FILE").filePath(f.getPath()).kind("file").startLine(1)
						.endLine(f.getLineCount()).score(0.5).repositoryId(f.getRepositoryId()).build());
			}
		}
		return out;
	}

	private List<SearchResultDto> filteredHits(Long projectId, List<Long> repoIds, SearchRequest request) {
		List<SearchResultDto> out = new ArrayList<>();
		if (request.getLanguage() != null) {
			for (RepositoryFile f : filesFor(projectId, repoIds)) {
				if (request.getLanguage().equalsIgnoreCase(f.getLanguage())) {
					out.add(SearchResultDto.builder().matchType("FILTER").filePath(f.getPath()).kind("file")
							.startLine(1).endLine(f.getLineCount()).score(0.3).repositoryId(f.getRepositoryId())
							.build());
				}
			}
		}

		List<CodeMethod> endpoints = methodRepository.findByProjectIdAndHttpPathNotNull(projectId).stream()
				.filter(m -> repoIds == null || repoIds.isEmpty() || repoIds.contains(m.getRepositoryId())).toList();
		for (CodeMethod m : endpoints) {
			String hay = (m.getName() + " " + (m.getHttpPath() == null ? "" : m.getHttpPath()))
					.toLowerCase(Locale.ROOT);
			if (hay.contains(request.getQuery().toLowerCase(Locale.ROOT))) {
				out.add(SearchResultDto.builder().matchType("API").filePath(pathOfMethod(m)).className(classNameOf(m))
						.methodName(m.getName()).kind("endpoint").startLine(m.getStartLine()).endLine(m.getEndLine())
						.score(0.7).repositoryId(m.getRepositoryId()).build());
			}
		}
		return out;
	}

	private String pathOfMethod(CodeMethod m) {
		return fileRepository.findById(m.getFileId()).map(RepositoryFile::getPath).orElse("");
	}

	private String classNameOf(CodeMethod m) {
		return classRepository.findById(m.getClassId()).map(CodeClass::getName).orElse("");
	}

	private List<RepositoryFile> filesFor(Long projectId, List<Long> repoIds) {
		if (repoIds == null || repoIds.isEmpty()) {
			return fileRepository.findAll().stream().filter(f -> f.getProjectId().equals(projectId)).toList();
		}
		List<RepositoryFile> out = new ArrayList<>();
		for (Long rid : repoIds) {
			out.addAll(fileRepository.findByProjectIdAndRepositoryIdOrderByPath(projectId, rid));
		}
		return out;
	}

	private SearchResultDto toResult(JpaCodeVectorStore.SearchHit h) {
		return SearchResultDto.builder().matchType("VECTOR").filePath(h.filePath()).className(h.className())
				.methodName(h.methodName()).kind(h.chunkType() == null ? "chunk" : h.chunkType().toLowerCase())
				.startLine(h.startLine()).endLine(h.endLine()).snippet(truncate(h.content(), 800)).score(h.score())
				.repositoryId(null).build();
	}

	private String truncate(String s, int max) {
		if (s == null) {
			return "";
		}
		return s.length() <= max ? s : s.substring(0, max);
	}
}
