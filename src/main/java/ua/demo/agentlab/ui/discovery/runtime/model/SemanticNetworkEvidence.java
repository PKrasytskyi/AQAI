package ua.demo.agentlab.ui.discovery.runtime.model;

public record SemanticNetworkEvidence(
        String evidenceId,
        String pageId,
        String pageUrl,
        String method,
        String endpoint,
        int status,
        String resourceType,
        String operation,
        String businessIntent,
        double confidence,
        String sourceTrace
) {
    public SemanticNetworkEvidence {
        evidenceId = clean(evidenceId);
        pageId = clean(pageId);
        pageUrl = clean(pageUrl);
        method = clean(method).toUpperCase(java.util.Locale.ROOT);
        endpoint = clean(endpoint);
        status = Math.max(0, status);
        resourceType = clean(resourceType);
        operation = clean(operation);
        businessIntent = clean(businessIntent);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
        sourceTrace = clean(sourceTrace);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
