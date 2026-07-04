package ua.demo.agentlab.ui.discovery.semantic.model;

import java.util.List;

public record SemanticPageModel(
        String pageId,
        String pageName,
        String route,
        String capability,
        List<SemanticElementModel> elements,
        List<ActionCandidate> pageActionCandidates,
        List<BusinessIntentCandidate> pageBusinessIntentCandidates,
        double confidence
) {
    public SemanticPageModel {
        pageId = safe(pageId);
        pageName = safe(pageName);
        route = safe(route);
        capability = safe(capability);
        elements = elements == null ? List.of() : List.copyOf(elements);
        pageActionCandidates = pageActionCandidates == null ? List.of() : List.copyOf(pageActionCandidates);
        pageBusinessIntentCandidates = pageBusinessIntentCandidates == null ? List.of() : List.copyOf(pageBusinessIntentCandidates);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
