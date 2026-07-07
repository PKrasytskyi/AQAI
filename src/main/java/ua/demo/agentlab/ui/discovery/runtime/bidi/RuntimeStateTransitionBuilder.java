package ua.demo.agentlab.ui.discovery.runtime.bidi;

import ua.demo.agentlab.ui.discovery.runtime.RuntimeEventNormalizer;
import ua.demo.agentlab.ui.discovery.runtime.model.NavigationEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeStateTransition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class RuntimeStateTransitionBuilder {

    private final RuntimeEventNormalizer normalizer = new RuntimeEventNormalizer();

    public List<RuntimeStateTransition> build(
            List<NavigationEvent> navigationEvents,
            List<BiDiRuntimeEvent> runtimeEvents
    ) {
        List<NavigationEvent> navigations = navigationEvents == null ? List.of() : navigationEvents;
        List<BiDiRuntimeEvent> events = runtimeEvents == null ? List.of() : runtimeEvents;
        if (navigations.isEmpty()) {
            return buildFromObservedRoutes(events);
        }
        AtomicInteger counter = new AtomicInteger(1);
        return navigations.stream()
                .map(event -> toTransition(event, events, counter.getAndIncrement()))
                .toList();
    }

    private RuntimeStateTransition toTransition(
            NavigationEvent event,
            List<BiDiRuntimeEvent> runtimeEvents,
            int index
    ) {
        String fromUrl = event.fromUrl();
        String toUrl = event.toUrl();
        boolean sameHost = normalizer.sameHost(fromUrl, toUrl);
        boolean routeChanged = !normalizer.path(fromUrl).equals(normalizer.path(toUrl));
        boolean sameDocument = sameDocument(event.trigger(), runtimeEvents, toUrl, sameHost, routeChanged);
        boolean networkIdle = hasEvent(runtimeEvents, toUrl, "network.idle");
        boolean lifecycleReady = hasLifecycleReady(runtimeEvents, toUrl);
        boolean domStableSignal = hasEvent(runtimeEvents, toUrl, "dom.mutation");
        boolean authNetwork = hasAuthNetworkEvidence(runtimeEvents);
        String type = sameDocument ? "SPA_ROUTE_CHANGE" : (routeChanged ? "DOCUMENT_NAVIGATION" : "STATE_REFRESH");
        double confidence = confidence(routeChanged, sameDocument, networkIdle, lifecycleReady, domStableSignal, authNetwork);
        return new RuntimeStateTransition(
                "runtime-transition-" + index,
                event.fromPageId(),
                fromUrl,
                event.toPageId(),
                toUrl,
                type,
                event.trigger(),
                routeChanged,
                sameDocument,
                confidence,
                "bidi-state-transition:" + event.eventId()
                        + ";networkIdle=" + networkIdle
                        + ";lifecycleReady=" + lifecycleReady
                        + ";domMutation=" + domStableSignal
                        + ";authNetwork=" + authNetwork
        );
    }

    private List<RuntimeStateTransition> buildFromObservedRoutes(List<BiDiRuntimeEvent> runtimeEvents) {
        if (runtimeEvents == null || runtimeEvents.isEmpty()) {
            return List.of();
        }
        List<BiDiRuntimeEvent> routeEvents = runtimeEvents.stream()
                .filter(event -> observedUrl(event) != null && !observedUrl(event).isBlank())
                .sorted(Comparator.comparingLong(BiDiRuntimeEvent::timestamp))
                .toList();
        List<RuntimeStateTransition> transitions = new ArrayList<>();
        String previousUrl = "";
        String previousPageId = "";
        int index = 1;
        for (BiDiRuntimeEvent event : routeEvents) {
            String currentUrl = observedUrl(event);
            if (previousUrl.isBlank()) {
                previousUrl = currentUrl;
                previousPageId = event.pageId();
                continue;
            }
            if (normalizer.path(previousUrl).equals(normalizer.path(currentUrl))) {
                continue;
            }
            boolean sameHost = normalizer.sameHost(previousUrl, currentUrl);
            boolean sameDocument = sameHost && routeTrigger(event.eventType(), event.attributes().get("trigger"));
            transitions.add(new RuntimeStateTransition(
                    "runtime-transition-" + index++,
                    previousPageId,
                    previousUrl,
                    event.pageId(),
                    currentUrl,
                    sameDocument ? "SPA_ROUTE_CHANGE" : "DOCUMENT_NAVIGATION",
                    firstNonBlank(event.attributes().get("trigger"), event.eventType()),
                    true,
                    sameDocument,
                    confidence(true, sameDocument, hasEvent(runtimeEvents, currentUrl, "network.idle"),
                            hasLifecycleReady(runtimeEvents, currentUrl), hasEvent(runtimeEvents, currentUrl, "dom.mutation"),
                            hasAuthNetworkEvidence(runtimeEvents)),
                    "bidi-state-transition:observed-route:" + event.eventType()
            ));
            previousUrl = currentUrl;
            previousPageId = event.pageId();
        }
        return transitions;
    }

    private boolean sameDocument(
            String trigger,
            List<BiDiRuntimeEvent> runtimeEvents,
            String toUrl,
            boolean sameHost,
            boolean routeChanged
    ) {
        if (!sameHost || !routeChanged) {
            return false;
        }
        if (routeTrigger("", trigger)) {
            return true;
        }
        return runtimeEvents.stream()
                .filter(event -> sameRoute(event, toUrl))
                .anyMatch(event -> routeTrigger(event.eventType(), event.attributes().get("trigger")));
    }

    private boolean hasLifecycleReady(List<BiDiRuntimeEvent> runtimeEvents, String url) {
        return runtimeEvents.stream()
                .filter(event -> sameRoute(event, url))
                .filter(event -> event.eventType().toLowerCase(Locale.ROOT).contains("lifecycle"))
                .anyMatch(event -> {
                    String name = firstNonBlank(event.attributes().get("name"), event.attributes().get("readyState"));
                    String normalized = name.toLowerCase(Locale.ROOT);
                    return normalized.contains("load")
                            || normalized.contains("domcontentloaded")
                            || normalized.contains("interactive")
                            || normalized.contains("complete");
                });
    }

    private boolean hasEvent(List<BiDiRuntimeEvent> runtimeEvents, String url, String eventType) {
        String normalizedType = clean(eventType).toLowerCase(Locale.ROOT);
        return runtimeEvents.stream()
                .filter(event -> sameRoute(event, url))
                .anyMatch(event -> event.eventType().toLowerCase(Locale.ROOT).equals(normalizedType)
                        || event.eventType().toLowerCase(Locale.ROOT).contains(normalizedType));
    }

    private boolean hasAuthNetworkEvidence(List<BiDiRuntimeEvent> runtimeEvents) {
        return runtimeEvents.stream()
                .filter(event -> event.eventType().toLowerCase(Locale.ROOT).contains("network"))
                .anyMatch(event -> {
                    String url = firstNonBlank(event.attributes().get("url"), event.pageUrl()).toLowerCase(Locale.ROOT);
                    String method = event.attributes().getOrDefault("method", "").toUpperCase(Locale.ROOT);
                    return method.equals("POST")
                            && (url.contains("auth") || url.contains("login") || url.contains("session")
                            || url.contains("token") || url.contains("validate"));
                });
    }

    private double confidence(
            boolean routeChanged,
            boolean sameDocument,
            boolean networkIdle,
            boolean lifecycleReady,
            boolean domStableSignal,
            boolean authNetwork
    ) {
        double value = sameDocument ? 0.82d : routeChanged ? 0.72d : 0.55d;
        if (networkIdle) {
            value += 0.08d;
        }
        if (lifecycleReady) {
            value += 0.05d;
        }
        if (domStableSignal) {
            value += 0.03d;
        }
        if (authNetwork) {
            value += 0.03d;
        }
        return Math.min(0.98d, value);
    }

    private boolean sameRoute(BiDiRuntimeEvent event, String url) {
        String targetPath = normalizer.path(url);
        if (targetPath.isBlank()) {
            return false;
        }
        String eventPath = normalizer.path(firstNonBlank(
                event.attributes().get("currentUrl"),
                event.attributes().get("pageUrl"),
                event.attributes().get("url"),
                event.pageUrl()
        ));
        return targetPath.equals(eventPath);
    }

    private String observedUrl(BiDiRuntimeEvent event) {
        return firstNonBlank(
                event.attributes().get("currentUrl"),
                event.attributes().get("pageUrl"),
                event.attributes().get("url"),
                event.pageUrl()
        );
    }

    private boolean routeTrigger(String eventType, String trigger) {
        String value = (clean(eventType) + " " + clean(trigger)).toLowerCase(Locale.ROOT);
        return value.contains("history.pushstate")
                || value.contains("history.replacestate")
                || value.contains("history.popstate")
                || value.contains("hashchange")
                || value.contains("navigationstarted");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
