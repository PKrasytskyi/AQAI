package ua.demo.agentlab.ui.discovery.runtime.model;

public record NetworkResponseEvent(
        String eventId,
        String pageId,
        String pageUrl,
        String method,
        String url,
        String path,
        int status,
        String resourceType,
        String timestamp,
        String source
) {
    public NetworkResponseEvent {
        eventId = clean(eventId);
        pageId = clean(pageId);
        pageUrl = clean(pageUrl);
        method = clean(method).toUpperCase(java.util.Locale.ROOT);
        url = clean(url);
        path = clean(path);
        status = Math.max(0, status);
        resourceType = clean(resourceType);
        timestamp = clean(timestamp);
        source = clean(source);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
