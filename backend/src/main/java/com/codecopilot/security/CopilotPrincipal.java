package com.codecopilot.security;

import com.codecopilot.user.RoleName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class CopilotPrincipal extends User {

    private final Long userId;
    private final Set<RoleName> roles;

    public CopilotPrincipal(Long userId, String username, String password, boolean enabled,
                            Collection<? extends GrantedAuthority> authorities, Set<RoleName> roles) {
        super(username, password, enabled, true, true, true, authorities);
        this.userId = userId;
        this.roles = roles;
    }

    public static CopilotPrincipal from(Long userId, String username, String password, Set<RoleName> roles) {
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .collect(Collectors.toSet());
        return new CopilotPrincipal(userId, username, password, true, authorities, roles);
    }

    public Long getUserId() {
        return userId;
    }

    public Set<RoleName> getRoles() {
        return roles;
    }

    public boolean hasRole(RoleName role) {
        return roles.contains(role);
    }
}