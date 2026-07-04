package ua.demo.agentlab.ui.discovery.runtime.model;

public record ConsoleLogEvent(
        String eventId,
        String pageId,
        String pageUrl,
        String level,
        String message,
        long timestamp,
        String source
) {
    public ConsoleLogEvent {
        eventId = clean(eventId);
        pageId = clean(pageId);
        pageUrl = clean(pageUrl);
        level = clean(level);
        message = clean(message);
        source = clean(source);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
