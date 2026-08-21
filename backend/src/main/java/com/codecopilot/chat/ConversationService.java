package com.codecopilot.chat;

import com.codecopilot.ai.AiService;
import com.codecopilot.ai.prompts.Prompts;
import com.codecopilot.chat.dto.ChatRequest;
import com.codecopilot.chat.dto.ChatResponse;
import com.codecopilot.chat.entity.Conversation;
import com.codecopilot.chat.entity.ConversationMessage;
import com.codecopilot.chat.repo.ConversationMessageRepository;
import com.codecopilot.chat.repo.ConversationRepository;
import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.common.security.SecurityUtils;
import com.codecopilot.project.Project;
import com.codecopilot.project.ProjectAccessService;
import com.codecopilot.project.ProjectRepository;
import com.codecopilot.project.dto.ProjectDto;
import com.codecopilot.project.ProjectService;
import com.codecopilot.rag.RetrievalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final RetrievalService retrievalService;
    private final AiService aiService;
    private final ProjectAccessService accessService;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMessageRepository messageRepository,
                               RetrievalService retrievalService, AiService aiService,
                               ProjectAccessService accessService, ProjectRepository projectRepository,
                               ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.retrievalService = retrievalService;
        this.aiService = aiService;
        this.accessService = accessService;
        this.projectRepository = projectRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Conversation> list(Long projectId) {
        Long userId = SecurityUtils.currentUserId();
        accessService.requireView(projectId);
        return conversationRepository.findByProjectIdAndUserIdOrderByLastMessageAtDesc(projectId, userId);
    }

    @Transactional
    public Conversation create(Long projectId, String title) {
        accessService.requireView(projectId);
        Conversation c = new Conversation();
        c.setProjectId(projectId);
        c.setUserId(SecurityUtils.currentUserId());
        c.setTitle(title == null || title.isBlank() ? "New conversation" : title);
        c.setLastMessageAt(Instant.now());
        return conversationRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> messages(Long projectId, Long conversationId) {
        accessService.requireView(projectId);
        Conversation conv = conversationRepository.findByIdAndProjectId(conversationId, projectId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId());
    }

    @Transactional
    public ChatResponse send(Long projectId, ChatRequest request) {
        accessService.requireView(projectId);
        Conversation conversation = resolveConversation(projectId, request.getConversationId());
        ConversationMessage userMsg = saveMessage(conversation, "user", request.getMessage(), null, null);
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        RetrievalService.RetrievalResult retrieval = retrievalService.retrieve(
                projectId, request.getRepositoryIds(), request.getMessage());
        String projectName = projectRepository.findById(projectId).map(Project::getName).orElse("?");
        String system = Prompts.chatSystem(projectName, projectTechnologies(projectId));
        String userPrompt = "Project question:\n" + request.getMessage() + "\n\nRetrieved repository context:\n" + retrieval.context();

        String answer = aiService.chat(system, userPrompt);
        ConversationMessage assistant = saveMessageWithSources(conversation, "assistant", answer, retrieval, null);

        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .messageId(assistant.getId())
                .content(answer)
                .sources(toSources(retrieval))
                .model(aiService.isConfigured() ? "grok" : "unavailable")
                .build();
    }

    public SseEmitter stream(Long projectId, ChatRequest request) {
        accessService.requireView(projectId);
        Conversation conversation = resolveConversation(projectId, request.getConversationId());
        saveMessage(conversation, "user", request.getMessage(), null, null);
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        RetrievalService.RetrievalResult retrieval = retrievalService.retrieve(
                projectId, request.getRepositoryIds(), request.getMessage());
        String projectName = projectRepository.findById(projectId).map(Project::getName).orElse("?");
        String system = Prompts.chatSystem(projectName, projectTechnologies(projectId));
        String userPrompt = "Project question:\n" + request.getMessage() + "\n\nRetrieved repository context:\n" + retrieval.context();

        SseEmitter emitter = new SseEmitter(120_000L);
        StringBuilder collected = new StringBuilder();
        aiService.stream(system, userPrompt)
                .doOnNext(token -> {
                    collected.append(token);
                    try {
                        emitter.send(SseEmitter.event().name("token").data(token));
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        emitter.send(SseEmitter.event().name("sources")
                                .data(jsonOf(toSources(retrieval))));
                        saveMessageWithSources(conversation, "assistant", collected.toString(), retrieval, null);
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnError(emitter::completeWithError)
                .subscribe();
        return emitter;
    }

    private Conversation resolveConversation(Long projectId, Long conversationId) {
        if (conversationId == null) {
            return create(projectId, "Conversation");
        }
        return conversationRepository.findByIdAndProjectId(conversationId, projectId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    private ConversationMessage saveMessage(Conversation conv, String role, String content, String sourcesJson, String toolsJson) {
        ConversationMessage m = new ConversationMessage();
        m.setConversationId(conv.getId());
        m.setProjectId(conv.getProjectId());
        m.setUserId(conv.getUserId());
        m.setRole(role);
        m.setContent(content);
        m.setSourcesJson(sourcesJson);
        m.setToolsJson(toolsJson);
        m.setModel(aiService.isConfigured() ? "grok" : null);
        return messageRepository.save(m);
    }

    private ConversationMessage saveMessageWithSources(Conversation conv, String role, String content,
                                                       RetrievalService.RetrievalResult retrieval, String toolsJson) {
        try {
            return saveMessage(conv, role, content, objectMapper.writeValueAsString(toSources(retrieval)), toolsJson);
        } catch (JsonProcessingException e) {
            return saveMessage(conv, role, content, "[]", toolsJson);
        }
    }

    private List<ChatResponse.Source> toSources(RetrievalService.RetrievalResult retrieval) {
        return retrieval.sources().stream().limit(8).map(s -> ChatResponse.Source.builder()
                .filePath(s.filePath())
                .className(s.className())
                .methodName(s.methodName())
                .startLine(s.startLine())
                .endLine(s.endLine())
                .snippet(truncate(s.snippet(), 500))
                .sourceType(s.sourceType())
                .score(s.score())
                .build()).toList();
    }

    private String jsonOf(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String projectTechnologies(Long projectId) {
        return projectRepository.findById(projectId)
                .map(p -> String.join(", ", p.getTechnologies()))
                .orElse("");
    }
}