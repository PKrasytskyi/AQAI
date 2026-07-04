package ua.demo.agentlab.ai.context;

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
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
