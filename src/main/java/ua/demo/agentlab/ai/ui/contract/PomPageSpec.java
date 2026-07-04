package ua.demo.agentlab.ai.ui.contract;

public record PomPageSpec(
        String name,
        String route,
        String capability,
        String openMethod
) {
    public PomPageSpec {
        name = safe(name);
        route = safe(route);
        capability = safe(capability);
        openMethod = safe(openMethod);
    }

    public static PomPageSpec empty() {
        return new PomPageSpec("", "", "", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
