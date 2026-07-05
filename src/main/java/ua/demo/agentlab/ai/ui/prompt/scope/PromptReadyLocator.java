package ua.demo.agentlab.ai.ui.prompt.scope;

import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;

public record PromptReadyLocator(
        String id,
        String elementName,
        String strategy,
        String value,
        String role,
        String componentName,
        String componentType,
        boolean sameOrigin,
        boolean uniqueWithinComponent,
        int globalMatchCount,
        int scopedMatchCount,
        double score,
        LocatorEvidenceType evidenceType,
        List<String> sourceTrace
) {
    public PromptReadyLocator(
            String id,
            String elementName,
            String strategy,
            String value,
            String role,
            String componentName,
            String componentType,
            boolean sameOrigin,
            boolean uniqueWithinComponent,
            int globalMatchCount,
            int scopedMatchCount,
            double score,
            List<String> sourceTrace
    ) {
        this(
                id,
                elementName,
                strategy,
                value,
                role,
                componentName,
                componentType,
                sameOrigin,
                uniqueWithinComponent,
                globalMatchCount,
                scopedMatchCount,
                score,
                LocatorEvidenceType.CANDIDATE_LOCATOR,
                sourceTrace
        );
    }

    public PromptReadyLocator {
        id = safe(id);
        elementName = safe(elementName);
        strategy = safe(strategy);
        value = safe(value);
        role = safe(role);
        componentName = safe(componentName);
        componentType = safe(componentType);
        globalMatchCount = Math.max(-1, globalMatchCount);
        scopedMatchCount = Math.max(-1, scopedMatchCount);
        score = Math.max(0.0d, Math.min(1.0d, score));
        evidenceType = evidenceType == null ? LocatorEvidenceType.CANDIDATE_LOCATOR : evidenceType;
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
