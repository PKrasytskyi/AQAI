package ua.demo.agentlab.ui.discovery.evidence;

import java.util.List;

public record POMRelevantEvidence(
        String pageId,
        String pageName,
        String route,
        String elementId,
        String componentName,
        String componentType,
        String fieldHint,
        String elementName,
        String strategy,
        String value,
        String role,
        String visibleText,
        String href,
        boolean sameOrigin,
        boolean uniqueWithinComponent,
        int globalMatchCount,
        int scopedMatchCount,
        double locatorScore,
        double semanticConfidence,
        double runtimeEvidenceConfidence,
        double requirementMatch,
        double routeMatch,
        double ownershipScore,
        double riskPenalty,
        double finalScore,
        LocatorEvidenceType evidenceType,
        List<String> risks,
        List<String> reasons
) {
    public POMRelevantEvidence(
            String pageId,
            String pageName,
            String route,
            String elementId,
            String componentName,
            String componentType,
            String fieldHint,
            String elementName,
            String strategy,
            String value,
            String role,
            String visibleText,
            String href,
            boolean sameOrigin,
            boolean uniqueWithinComponent,
            int globalMatchCount,
            int scopedMatchCount,
            double locatorScore,
            double semanticConfidence,
            double runtimeEvidenceConfidence,
            double requirementMatch,
            double routeMatch,
            double ownershipScore,
            double riskPenalty,
            double finalScore,
            List<String> risks,
            List<String> reasons
    ) {
        this(
                pageId,
                pageName,
                route,
                elementId,
                componentName,
                componentType,
                fieldHint,
                elementName,
                strategy,
                value,
                role,
                visibleText,
                href,
                sameOrigin,
                uniqueWithinComponent,
                globalMatchCount,
                scopedMatchCount,
                locatorScore,
                semanticConfidence,
                runtimeEvidenceConfidence,
                requirementMatch,
                routeMatch,
                ownershipScore,
                riskPenalty,
                finalScore,
                LocatorEvidenceType.CANDIDATE_LOCATOR,
                risks,
                reasons
        );
    }

    public POMRelevantEvidence {
        pageId = safe(pageId);
        pageName = safe(pageName);
        route = safe(route);
        elementId = safe(elementId);
        componentName = safe(componentName);
        componentType = safe(componentType);
        fieldHint = safe(fieldHint);
        elementName = safe(elementName);
        strategy = safe(strategy);
        value = safe(value);
        role = safe(role);
        visibleText = safe(visibleText);
        href = safe(href);
        globalMatchCount = Math.max(-1, globalMatchCount);
        scopedMatchCount = Math.max(-1, scopedMatchCount);
        locatorScore = clamp(locatorScore);
        semanticConfidence = clamp(semanticConfidence);
        runtimeEvidenceConfidence = clamp(runtimeEvidenceConfidence);
        requirementMatch = clamp(requirementMatch);
        routeMatch = clamp(routeMatch);
        ownershipScore = clamp(ownershipScore);
        riskPenalty = clamp(riskPenalty);
        finalScore = clamp(finalScore);
        evidenceType = evidenceType == null ? LocatorEvidenceType.CANDIDATE_LOCATOR : evidenceType;
        risks = risks == null ? List.of() : List.copyOf(risks);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : 0.0d;
    }
}
