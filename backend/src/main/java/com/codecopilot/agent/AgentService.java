package com.codecopilot.agent;

import com.codecopilot.ai.prompts.Prompts;
import com.codecopilot.ai.tools.CodeIntelligenceTools;
import com.codecopilot.ai.tools.CurrentProjectContext;
import com.codecopilot.common.exception.BadRequestException;
import com.codecopilot.common.security.SecurityUtils;
import com.codecopilot.project.Project;
import com.codecopilot.project.ProjectAccessService;
import com.codecopilot.project.ProjectRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentService {

    private final ChatClient.Builder chatClientBuilder;
    private final CodeIntelligenceTools tools;
    private final ProjectAccessService accessService;
    private final ProjectRepository projectRepository;
    private final com.codecopilot.ai.tools.ToolExecutionRepository executionRepository;

    public AgentService(ChatClient.Builder chatClientBuilder, CodeIntelligenceTools tools,
                        ProjectAccessService accessService, ProjectRepository projectRepository,
                        com.codecopilot.ai.tools.ToolExecutionRepository executionRepository) {
        this.chatClientBuilder = chatClientBuilder;
        this.tools = tools;
        this.accessService = accessService;
        this.projectRepository = projectRepository;
        this.executionRepository = executionRepository;
    }

    public AgentResponse run(Long projectId, String prompt) {
        accessService.requireView(projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not found"));
        CurrentProjectContext.set(new CurrentProjectContext.CurrentProject(projectId, SecurityUtils.currentUserId()));
        try {
            String system = Prompts.agentSystem(project.getName());
            ChatClient chatClient = chatClientBuilder.build();
            String answer = chatClient.prompt()
                    .system(system)
                    .user(prompt)
                    .tools(tools)
                    .call()
                    .content();
            List<String> toolCalls = executionRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                    .limit(10)
                    .map(e -> e.getToolName() + " (input: " + e.getInputJson() + ")")
                    .toList();
            return new AgentResponse(prompt, answer, toolCalls);
        } finally {
            CurrentProjectContext.clear();
        }
    }

    public record AgentResponse(String prompt, String answer, List<String> toolCalls) {
    }
}