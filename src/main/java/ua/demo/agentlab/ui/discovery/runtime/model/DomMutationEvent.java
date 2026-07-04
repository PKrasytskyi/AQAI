package ua.demo.agentlab.ui.discovery.runtime.model;

public record DomMutationEvent(
        String eventId,
        String pageId,
        String pageUrl,
        String mutationType,
        String target,
        int count,
        String timestamp,
        String source
) {
    public DomMutationEvent {
        eventId = clean(eventId);
        pageId = clean(pageId);
        pageUrl = clean(pageUrl);
        mutationType = clean(mutationType);
        target = clean(target);
        count = Math.max(0, count);
        timestamp = clean(timestamp);
        source = clean(source);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
