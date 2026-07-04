package ua.demo.agentlab.ui.discovery.runtime;

import ua.demo.agentlab.ui.discovery.runtime.model.NavigationEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeStateTransition;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SpaStateTransitionDetector {

    private final RuntimeEventNormalizer normalizer = new RuntimeEventNormalizer();

    public List<RuntimeStateTransition> detect(List<NavigationEvent> navigationEvents) {
        if (navigationEvents == null || navigationEvents.isEmpty()) {
            return List.of();
        }
        AtomicInteger counter = new AtomicInteger(1);
        return navigationEvents.stream()
                .map(event -> toTransition(event, counter.getAndIncrement()))
                .toList();
    }

    private RuntimeStateTransition toTransition(NavigationEvent event, int index) {
        boolean sameHost = normalizer.sameHost(event.fromUrl(), event.toUrl());
        boolean routeChanged = !normalizer.path(event.fromUrl()).equals(normalizer.path(event.toUrl()));
        boolean sameDocument = sameHost && routeChanged && !looksLikeFullDocumentNavigation(event.trigger());
        String type = sameDocument ? "SPA_ROUTE_CHANGE" : (routeChanged ? "DOCUMENT_NAVIGATION" : "STATE_REFRESH");
        double confidence = sameDocument ? 0.80d : routeChanged ? 0.70d : 0.55d;
        return new RuntimeStateTransition(
                "runtime-transition-" + index,
                event.fromPageId(),
                event.fromUrl(),
                event.toPageId(),
                event.toUrl(),
                type,
                event.trigger(),
                routeChanged,
                sameDocument,
                confidence,
                "navigation-event:" + event.eventId()
        );
    }

    private boolean looksLikeFullDocumentNavigation(String trigger) {
        String normalized = trigger == null ? "" : trigger.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("document") || normalized.contains("open") || normalized.contains("direct");
    }
}
