package com.codecopilot.project;

import com.codecopilot.common.exception.BadRequestException;
import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.common.security.SecurityUtils;
import com.codecopilot.project.dto.AddMemberRequest;
import com.codecopilot.project.dto.ProjectDto;
import com.codecopilot.project.dto.ProjectRequest;
import com.codecopilot.user.RoleName;
import com.codecopilot.user.User;
import com.codecopilot.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService accessService;
    private final ProjectDashboardService dashboardService;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
                          UserRepository userRepository, ProjectAccessService accessService,
                          ProjectDashboardService dashboardService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.dashboardService = dashboardService;
    }

    @Transactional
    public ProjectDto create(ProjectRequest request) {
        if (projectRepository.existsByName(request.getName())) {
            throw new BadRequestException("A project with this name already exists");
        }
        Long userId = SecurityUtils.currentUserId();
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setTechnologies(request.getTechnologies() == null ? Set.of() : request.getTechnologies());
        project.setOwnerId(userId);
        projectRepository.save(project);
        return toDto(project, ProjectRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> listMine() {
        Long userId = SecurityUtils.currentUserId();
        Map<Long, ProjectRole> roles = new java.util.LinkedHashMap<>();
        for (Project p : projectRepository.findByOwnerIdOrderByUpdatedAtDesc(userId)) {
            roles.put(p.getId(), ProjectRole.OWNER);
        }
        for (ProjectMember member : memberRepository.findByUserId(userId)) {
            roles.putIfAbsent(member.getProjectId(), member.getRole());
        }
        if (SecurityUtils.hasRole(RoleName.ADMIN.name())) {
            for (Project p : projectRepository.findAll()) {
                roles.putIfAbsent(p.getId(), ProjectRole.OWNER);
            }
        }
        return roles.entrySet().stream()
                .map(e -> {
                    Project p = projectRepository.findById(e.getKey()).orElse(null);
                    if (p == null) {
                        return null;
                    }
                    return toDto(p, e.getValue());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto get(Long projectId) {
        ProjectAccessService.ProjectAccess access = accessService.requireProject(projectId);
        return toDto(access.project(), access.role());
    }

    @Transactional
    public ProjectDto update(Long projectId, ProjectRequest request) {
        accessService.requireEdit(projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        if (request.getName() != null && !request.getName().isBlank()) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getTechnologies() != null) {
            project.setTechnologies(request.getTechnologies());
        }
        return toDto(projectRepository.save(project), accessService.effectiveRole(project, SecurityUtils.currentUserId()));
    }

    @Transactional
    public void delete(Long projectId) {
        accessService.requireOwner(projectId);
        memberRepository.findByProjectId(projectId).forEach(memberRepository::delete);
        projectRepository.deleteById(projectId);
    }

    @Transactional
    public void addMember(Long projectId, AddMemberRequest request) {
        accessService.requireOwner(projectId);
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        memberRepository.findByProjectIdAndUserId(projectId, user.getId()).ifPresent(memberRepository::delete);
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(user.getId());
        member.setRole(ProjectRole.valueOf(request.getRole().name()));
        memberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        accessService.requireOwner(projectId);
        memberRepository.findByProjectIdAndUserId(projectId, userId).ifPresent(memberRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto.ProjectMemberDto> members(Long projectId) {
        accessService.requireView(projectId);
        return memberRepository.findByProjectId(projectId).stream().map(m -> {
            User u = userRepository.findById(m.getUserId()).orElse(null);
            return ProjectDto.ProjectMemberDto.builder()
                    .userId(m.getUserId())
                    .username(u == null ? "unknown" : u.getUsername())
                    .displayName(u == null ? "unknown" : u.getDisplayName())
                    .role(m.getRole().name())
                    .build();
        }).toList();
    }

    public Object dashboard(Long projectId) {
        accessService.requireView(projectId);
        return dashboardService.build(projectId);
    }

    private ProjectDto toDto(Project project, ProjectRole role) {
        User owner = userRepository.findById(project.getOwnerId()).orElse(null);
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .technologies(project.getTechnologies())
                .ownerId(project.getOwnerId())
                .ownerUsername(owner == null ? "unknown" : owner.getUsername())
                .access(ProjectDto.ProjectRoleDto.valueOf(role.name()))
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}