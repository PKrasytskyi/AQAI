package ua.demo.agentlab.ui.discovery.runtime.model;

public record NetworkRequestEvent(
        String eventId,
        String pageId,
        String pageUrl,
        String method,
        String url,
        String path,
        String resourceType,
        String timestamp,
        String source
) {
    public NetworkRequestEvent {
        eventId = clean(eventId);
        pageId = clean(pageId);
        pageUrl = clean(pageUrl);
        method = clean(method).toUpperCase(java.util.Locale.ROOT);
        url = clean(url);
        path = clean(path);
        resourceType = clean(resourceType);
        timestamp = clean(timestamp);
        source = clean(source);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
