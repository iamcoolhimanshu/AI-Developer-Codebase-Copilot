package com.codecopilot.common.security;

import com.codecopilot.common.exception.ForbiddenException;
import com.codecopilot.security.CopilotPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails ud)) {
            throw new ForbiddenException("Not authenticated");
        }
        if (ud instanceof CopilotPrincipal principal) {
            return principal.getUserId();
        }
        try {
            return Long.valueOf(ud.getUsername());
        } catch (NumberFormatException e) {
            throw new ForbiddenException("Unknown principal: " + ud.getUsername());
        }
    }

    public static String currentUserLogin() {
        return currentUserId().toString();
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}