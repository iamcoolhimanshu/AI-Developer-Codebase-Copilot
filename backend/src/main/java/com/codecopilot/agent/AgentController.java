package com.codecopilot.agent;

import com.codecopilot.common.api.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/agent")
public class AgentController {

	private final AgentService agentService;

	public AgentController(AgentService agentService) {
		this.agentService = agentService;
	}

	@PostMapping("/run")
	public ApiResponse<AgentService.AgentResponse> run(@PathVariable Long projectId,
			@RequestBody AgentRequest request) {
		return ApiResponse.ok(agentService.run(projectId, request.getPrompt()));
	}

	@Data
	public static class AgentRequest {
		@NotBlank
		private String prompt;
	}
}