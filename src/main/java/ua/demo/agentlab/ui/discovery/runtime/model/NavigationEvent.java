package ua.demo.agentlab.ui.discovery.runtime.model;

public record NavigationEvent(
        String eventId,
        String fromPageId,
        String fromUrl,
        String toPageId,
        String toUrl,
        String trigger,
        boolean success,
        String source
) {
    public NavigationEvent {
        eventId = clean(eventId);
        fromPageId = clean(fromPageId);
        fromUrl = clean(fromUrl);
        toPageId = clean(toPageId);
        toUrl = clean(toUrl);
        trigger = clean(trigger);
        source = clean(source);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
