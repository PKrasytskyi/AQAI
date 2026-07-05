package ua.demo.agentlab.ui.discovery.runtime.bidi;

import ua.demo.agentlab.ui.discovery.runtime.RuntimeEventNormalizer;
import ua.demo.agentlab.ui.discovery.runtime.SpaStateTransitionDetector;
import ua.demo.agentlab.ui.discovery.runtime.model.ConsoleLogEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.DomMutationEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.NavigationEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.NetworkRequestEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.NetworkResponseEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BiDiEventNormalizer {

    private final RuntimeEventNormalizer normalizer = new RuntimeEventNormalizer();
    private final SpaStateTransitionDetector stateTransitionDetector = new SpaStateTransitionDetector();

    public RuntimeEvidenceBundle normalize(List<BiDiRuntimeEvent> events) {
        if (events == null || events.isEmpty()) {
            return RuntimeEvidenceBundle.empty();
        }
        List<NetworkRequestEvent> networkRequests = new ArrayList<>();
        List<NetworkResponseEvent> networkResponses = new ArrayList<>();
        List<ConsoleLogEvent> consoleLogs = new ArrayList<>();
        List<NavigationEvent> navigationEvents = new ArrayList<>();
        List<DomMutationEvent> domMutations = new ArrayList<>();
        int networkIndex = 1;
        int consoleIndex = 1;
        int navigationIndex = 1;
        int mutationIndex = 1;
        for (BiDiRuntimeEvent event : events) {
            String type = event.eventType().toLowerCase(Locale.ROOT);
            if (type.contains("network") && type.contains("request")) {
                String url = event.attributes().getOrDefault("url", "");
                String eventId = "bidi-network-" + networkIndex++;
                networkRequests.add(new NetworkRequestEvent(
                        eventId + "-request",
                        event.pageId(),
                        event.pageUrl(),
                        event.attributes().getOrDefault("method", "GET"),
                        url,
                        normalizer.path(url),
                        event.attributes().getOrDefault("resourceType", "XHR"),
                        String.valueOf(event.timestamp()),
                        "bidi"
                ));
            } else if (type.contains("network")) {
                String url = event.attributes().getOrDefault("url", "");
                String eventId = "bidi-network-" + networkIndex++;
                networkResponses.add(new NetworkResponseEvent(
                        eventId + "-response",
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
            } else if (type.contains("navigation")) {
                String fromUrl = event.attributes().getOrDefault("fromUrl", event.pageUrl());
                String toUrl = event.attributes().getOrDefault("url", event.pageUrl());
                navigationEvents.add(new NavigationEvent(
                        "bidi-navigation-" + navigationIndex++,
                        pageIdFromUrl(fromUrl, event.pageId()),
                        fromUrl,
                        pageIdFromUrl(toUrl, event.pageId()),
                        toUrl,
                        event.attributes().getOrDefault("trigger", event.eventType()),
                        true,
                        "bidi"
                ));
            } else if (type.contains("mutation")) {
                domMutations.add(new DomMutationEvent(
                        "bidi-dom-mutation-" + mutationIndex++,
                        event.pageId(),
                        event.pageUrl(),
                        event.attributes().getOrDefault("mutationType", "unknown"),
                        event.attributes().getOrDefault("target", "document"),
                        parseInt(event.attributes().get("count")),
                        String.valueOf(event.timestamp()),
                        "bidi"
                ));
            }
        }
        return new RuntimeEvidenceBundle(
                networkRequests,
                networkResponses,
                consoleLogs,
                navigationEvents,
                domMutations,
                List.of(),
                stateTransitionDetector.detect(navigationEvents),
                List.of(
                        "collector=bidi-event-normalizer",
                        "rawEvents=" + events.size(),
                        "networkRequests=" + networkRequests.size(),
                        "networkResponses=" + networkResponses.size(),
                        "navigationEvents=" + navigationEvents.size(),
                        "domMutations=" + domMutations.size()
                )
        );
    }

    private int parseInt(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String pageIdFromUrl(String url, String fallback) {
        try {
            java.net.URI uri = java.net.URI.create(url == null ? "" : url.trim());
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return "home-page";
            }
            String normalized = path.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-+|-+$", "");
            return normalized.isBlank() ? fallback : normalized.toLowerCase(Locale.ROOT);
        } catch (Exception exception) {
            return fallback;
        }
    }
}
