package com.codecopilot.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberRequest {

    @NotNull
    private Long userId;

    @NotNull
    private ProjectRoleDto role;

    public enum ProjectRoleDto {
        OWNER, DEVELOPER, VIEWER
    }
}