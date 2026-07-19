package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

/** A browser-observed SPA state, distinct from a route or static page inventory node. */
public record UiStateSnapshot(
        String stateId,
        String pageId,
        String route,
        String stateFingerprint,
        KnowledgeRunMetadata runMetadata,
        List<String> visibleComponentIds,
        List<String> overlaySignatures,
        List<String> menuSignatures,
        List<String> modalSignatures,
        boolean loading,
        boolean networkIdle,
        boolean authenticated,
        double confidence,
        List<String> sourceTrace
) {
    public UiStateSnapshot {
        stateId = safe(stateId);
        pageId = safe(pageId);
        route = safe(route);
        stateFingerprint = safe(stateFingerprint);
        visibleComponentIds = visibleComponentIds == null ? List.of() : List.copyOf(visibleComponentIds);
        overlaySignatures = overlaySignatures == null ? List.of() : List.copyOf(overlaySignatures);
        menuSignatures = menuSignatures == null ? List.of() : List.copyOf(menuSignatures);
        modalSignatures = modalSignatures == null ? List.of() : List.copyOf(modalSignatures);
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
