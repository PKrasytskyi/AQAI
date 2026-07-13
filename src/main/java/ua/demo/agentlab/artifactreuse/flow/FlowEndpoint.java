package ua.demo.agentlab.artifactreuse.flow;

public record FlowEndpoint(String pageName, String route) {
    public FlowEndpoint {
        pageName = safe(pageName);
        route = safe(route);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
