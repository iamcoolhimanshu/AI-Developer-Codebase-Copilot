package com.codecopilot.search;

import com.codecopilot.common.api.ApiResponse;
import com.codecopilot.project.ProjectAccessService;
import com.codecopilot.search.dto.SearchRequest;
import com.codecopilot.search.dto.SearchResultDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class SearchController {

    private final SearchService searchService;
    private final ProjectAccessService accessService;

    public SearchController(SearchService searchService, ProjectAccessService accessService) {
        this.searchService = searchService;
        this.accessService = accessService;
    }

    @GetMapping("/search")
    public ApiResponse<List<SearchResultDto>> search(@PathVariable Long projectId,
                                                     @RequestParam String query,
                                                     @RequestParam(required = false) String type,
                                                     @RequestParam(required = false) String language,
                                                     @RequestParam(required = false) Long repositoryId,
                                                     @RequestParam(required = false) Integer topK) {
        accessService.requireView(projectId);
        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setTopK(topK == null ? 20 : topK);
        request.setLanguage(language);
        request.setRepositoryIds(repositoryId == null ? null : List.of(repositoryId));
        if (type != null && !type.isBlank()) {
            request.setTypes(List.of(type.split(",")));
        }
        return ApiResponse.ok(searchService.search(projectId, request));
    }
}