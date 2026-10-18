package com.tracenixa.platform.project.context;

public final class ProjectContext {

    private static final ThreadLocal<String> CURRENT_PROJECT_ID = new ThreadLocal<>();

    private ProjectContext() {}

    public static void setProjectId(String projectId) {
        CURRENT_PROJECT_ID.set(projectId);
    }

    public static String getProjectId() {
        return CURRENT_PROJECT_ID.get();
    }

    public static void clear() {
        CURRENT_PROJECT_ID.remove();
    }
}
