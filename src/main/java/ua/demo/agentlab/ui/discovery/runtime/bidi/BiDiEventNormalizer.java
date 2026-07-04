package ua.demo.agentlab.ui.discovery.runtime.bidi;

import ua.demo.agentlab.ui.discovery.runtime.RuntimeEventNormalizer;
import ua.demo.agentlab.ui.discovery.runtime.model.ConsoleLogEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.NetworkResponseEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BiDiEventNormalizer {

    private final RuntimeEventNormalizer normalizer = new RuntimeEventNormalizer();

    public RuntimeEvidenceBundle normalize(List<BiDiRuntimeEvent> events) {
        if (events == null || events.isEmpty()) {
            return RuntimeEvidenceBundle.empty();
        }
        List<NetworkResponseEvent> networkResponses = new ArrayList<>();
        List<ConsoleLogEvent> consoleLogs = new ArrayList<>();
        int networkIndex = 1;
        int consoleIndex = 1;
        for (BiDiRuntimeEvent event : events) {
            String type = event.eventType().toLowerCase(Locale.ROOT);
            if (type.contains("network")) {
                String url = event.attributes().getOrDefault("url", "");
                networkResponses.add(new NetworkResponseEvent(
                        "bidi-network-" + networkIndex++,
                        event.pageId(),
                        event.pageUrl(),
                        event.attributes().getOrDefault("method", "GET"),
                        url,
                        normalizer.path(url),
                        parseInt(event.attributes().get("status")),
                        event.attributes().getOrDefault("resourceType", "XHR"),
                        String.valueOf(event.timestamp()),
                        "bidi"
                ));
            } else if (type.contains("log") || type.contains("console")) {
                consoleLogs.add(new ConsoleLogEvent(
                        "bidi-console-" + consoleIndex++,
                        event.pageId(),
                        event.pageUrl(),
                        event.attributes().getOrDefault("level", "INFO"),
                        event.attributes().getOrDefault("message", ""),
                        event.timestamp(),
                        "bidi"
                ));
            }
        }
        return new RuntimeEvidenceBundle(
                List.of(),
                networkResponses,
                consoleLogs,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("collector=bidi-event-normalizer", "rawEvents=" + events.size())
        );
    }

    private int parseInt(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
