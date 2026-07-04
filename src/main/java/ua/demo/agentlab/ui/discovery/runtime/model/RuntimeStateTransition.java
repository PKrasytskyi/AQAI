package ua.demo.agentlab.ui.discovery.runtime.model;

public record RuntimeStateTransition(
        String transitionId,
        String fromPageId,
        String fromUrl,
        String toPageId,
        String toUrl,
        String transitionType,
        String trigger,
        boolean routeChanged,
        boolean sameDocument,
        double confidence,
        String sourceTrace
) {
    public RuntimeStateTransition {
        transitionId = clean(transitionId);
        fromPageId = clean(fromPageId);
        fromUrl = clean(fromUrl);
        toPageId = clean(toPageId);
        toUrl = clean(toUrl);
        transitionType = clean(transitionType);
        trigger = clean(trigger);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
        sourceTrace = clean(sourceTrace);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
