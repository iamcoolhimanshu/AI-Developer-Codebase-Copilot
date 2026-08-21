package com.codecopilot.ai.tools;

import com.codecopilot.ai.tools.CurrentProjectContext.CurrentProject;

/**
 * Request-scoped holder for the project context an AI tool runs within.
 * Spring AI tool calls execute synchronously on the calling thread, so a
 * thread-local is a safe and simple way to scope tool access to the project
 * the user is working in (backend-enforced authorization).
 */
public final class CurrentProjectContext {

    private static final ThreadLocal<CurrentProject> HOLDER = new ThreadLocal<>();

    private CurrentProjectContext() {
    }

    public record CurrentProject(Long projectId, Long userId) {
    }

    public static void set(CurrentProject project) {
        HOLDER.set(project);
    }

    public static CurrentProject get() {
        return HOLDER.get();
    }

    public static Long projectId() {
        CurrentProject cp = HOLDER.get();
        return cp == null ? null : cp.projectId();
    }

    public static Long userId() {
        CurrentProject cp = HOLDER.get();
        return cp == null ? null : cp.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}