package ua.demo.agentlab.ai.pageenrichment.model;

public record PageModelEnrichmentFailure(
        String pageId,
        String pageName,
        String route,
        String error,
        String response
) {
    public PageModelEnrichmentFailure {
        pageId = clean(pageId);
        pageName = clean(pageName);
        route = clean(route);
        error = clean(error);
        response = response == null ? "" : response.trim();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
