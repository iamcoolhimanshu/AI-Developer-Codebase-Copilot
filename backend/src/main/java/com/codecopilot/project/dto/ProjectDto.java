package com.codecopilot.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {

    private Long id;
    private String name;
    private String description;
    private Set<String> technologies;
    private Long ownerId;
    private String ownerUsername;
    private ProjectRoleDto access;
    private Instant createdAt;
    private Instant updatedAt;

    public enum ProjectRoleDto {
        OWNER, DEVELOPER, VIEWER
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectMemberDto {
        private Long userId;
        private String username;
        private String displayName;
        private String role;
    }
}