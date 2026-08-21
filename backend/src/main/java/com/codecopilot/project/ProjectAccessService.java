package com.codecopilot.project;

import com.codecopilot.common.exception.ForbiddenException;
import com.codecopilot.common.exception.NotFoundException;
import com.codecopilot.common.security.SecurityUtils;
import com.codecopilot.user.RoleName;
import com.codecopilot.user.User;
import com.codecopilot.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    public ProjectAccessService(ProjectRepository projectRepository,
                                ProjectMemberRepository memberRepository,
                                UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    public record ProjectAccess(Project project, ProjectRole role) {
    }

    public ProjectAccess requireProject(Long projectId) {
        return requireProject(projectId, SecurityUtils.currentUserId());
    }

    public ProjectAccess requireProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
        ProjectRole role = effectiveRole(project, userId);
        return new ProjectAccess(project, role);
    }

    public ProjectRole effectiveRole(Project project, Long userId) {
        if (project.getOwnerId().equals(userId)) {
            return ProjectRole.OWNER;
        }
        if (isGlobalAdmin(userId)) {
            return ProjectRole.OWNER;
        }
        Optional<ProjectMember> member = memberRepository.findByProjectIdAndUserId(project.getId(), userId);
        return member.map(ProjectMember::getRole).orElse(ProjectRole.VIEWER);
    }

    public boolean canView(Long projectId) {
        try {
            requireProject(projectId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void requireView(Long projectId) {
        requireProject(projectId);
    }

    public void requireEdit(Long projectId) {
        ProjectAccess access = requireProject(projectId);
        if (access.role() == ProjectRole.VIEWER && !isGlobalAdmin(SecurityUtils.currentUserId())) {
            throw new ForbiddenException("You need edit access to this project");
        }
    }

    public void requireOwner(Long projectId) {
        ProjectAccess access = requireProject(projectId);
        if (access.role() != ProjectRole.OWNER) {
            throw new ForbiddenException("Only the project owner can perform this action");
        }
    }

    public boolean isGlobalAdmin(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN);
    }
}