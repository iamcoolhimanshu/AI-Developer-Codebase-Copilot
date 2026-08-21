package com.codecopilot.auth.dto;

import com.codecopilot.user.RoleName;

import java.util.Set;

public record CurrentUserDto(Long id, String username, String email, String displayName, Set<RoleName> roles) {
}