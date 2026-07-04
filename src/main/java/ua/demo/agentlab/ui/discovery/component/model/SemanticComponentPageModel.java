package ua.demo.agentlab.ui.discovery.component.model;

import java.util.List;

public record SemanticComponentPageModel(
        String pageId,
        String route,
        String featureGuess,
        List<SemanticComponentModel> components,
        double confidence
) {
    public SemanticComponentPageModel {
        pageId = safe(pageId);
        route = safe(route);
        featureGuess = safe(featureGuess);
        components = components == null ? List.of() : List.copyOf(components);
        confidence = clamp(confidence);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : 0.0d;
    }
}
