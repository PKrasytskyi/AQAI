package ua.demo.agentlab.ui.discovery.evidence.funnel;

/** Page/state identity resolved from current-run binding and transition evidence. */
public record UiEvidenceStateReference(
        String pageId,
        String pageName,
        String route,
        String stateId,
        String routeSource,
        boolean confirmed
) {
    public UiEvidenceStateReference {
        pageId = safe(pageId);
        pageName = safe(pageName);
        route = safe(route);
        stateId = safe(stateId);
        routeSource = safe(routeSource);
    }

    public static UiEvidenceStateReference empty() {
        return new UiEvidenceStateReference("", "", "", "", "UNRESOLVED", false);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
