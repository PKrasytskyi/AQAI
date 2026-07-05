package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;

public record PromptLocatorEvidence(
        String fieldHint,
        String elementName,
        String strategy,
        String value,
        String role,
        String visibleText,
        String href,
        boolean sameOrigin,
        double stabilityScore,
        String componentName,
        String componentType,
        int globalMatchCount,
        int scopedMatchCount,
        boolean uniqueWithinComponent,
        LocatorEvidenceType evidenceType,
        List<String> sourceTrace
) {
    public PromptLocatorEvidence(
            String fieldHint,
            String elementName,
            String strategy,
            String value,
            String role,
            String visibleText,
            String href,
            boolean sameOrigin,
            double stabilityScore,
            List<String> sourceTrace
    ) {
        this(
                fieldHint,
                elementName,
                strategy,
                value,
                role,
                visibleText,
                href,
                sameOrigin,
                stabilityScore,
                "",
                "",
                -1,
                -1,
                false,
                inferEvidenceType(sameOrigin, stabilityScore, false, -1, -1),
                sourceTrace
        );
    }

    public PromptLocatorEvidence(
            String fieldHint,
            String elementName,
            String strategy,
            String value,
            String role,
            String visibleText,
            String href,
            boolean sameOrigin,
            double stabilityScore,
            String componentName,
            String componentType,
            int globalMatchCount,
            int scopedMatchCount,
            boolean uniqueWithinComponent,
            List<String> sourceTrace
    ) {
        this(
                fieldHint,
                elementName,
                strategy,
                value,
                role,
                visibleText,
                href,
                sameOrigin,
                stabilityScore,
                componentName,
                componentType,
                globalMatchCount,
                scopedMatchCount,
                uniqueWithinComponent,
                inferEvidenceType(sameOrigin, stabilityScore, uniqueWithinComponent, globalMatchCount, scopedMatchCount),
                sourceTrace
        );
    }

    public PromptLocatorEvidence {
        fieldHint = safe(fieldHint);
        elementName = safe(elementName);
        strategy = safe(strategy);
        value = safe(value);
        role = safe(role);
        visibleText = safe(visibleText);
        href = safe(href);
        stabilityScore = Math.max(0.0d, Math.min(1.0d, stabilityScore));
        componentName = safe(componentName);
        componentType = safe(componentType);
        globalMatchCount = Math.max(-1, globalMatchCount);
        scopedMatchCount = Math.max(-1, scopedMatchCount);
        evidenceType = evidenceType == null ? LocatorEvidenceType.CANDIDATE_LOCATOR : evidenceType;
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static LocatorEvidenceType inferEvidenceType(
            boolean sameOrigin,
            double score,
            boolean uniqueWithinComponent,
            int globalMatchCount,
            int scopedMatchCount
    ) {
        if (sameOrigin
                && score >= 0.75d
                && (uniqueWithinComponent || globalMatchCount == 1 || scopedMatchCount == 1)
                && (globalMatchCount >= 0 || scopedMatchCount >= 0)) {
            return LocatorEvidenceType.CONFIRMED_LOCATOR;
        }
        if (sameOrigin && score >= 0.45d) {
            return LocatorEvidenceType.CANDIDATE_LOCATOR;
        }
        return LocatorEvidenceType.FALLBACK_LOCATOR;
    }
}
