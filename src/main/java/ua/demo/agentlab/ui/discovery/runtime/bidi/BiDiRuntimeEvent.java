package ua.demo.agentlab.ui.discovery.runtime.bidi;

import java.util.Map;

public record BiDiRuntimeEvent(
        String eventType,
        String pageId,
        String pageUrl,
        Map<String, String> attributes,
        long timestamp
) {
    public BiDiRuntimeEvent {
        eventType = eventType == null ? "" : eventType.trim();
        pageId = pageId == null ? "" : pageId.trim();
        pageUrl = pageUrl == null ? "" : pageUrl.trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
