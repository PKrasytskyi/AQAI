package ua.demo.agentlab.ai.context;

import java.util.List;

public record CanonicalUiInteraction(
        String interactionId,
        String pageId,
        String pageName,
        String canonicalName,
        String interactionType,
        String subjectType,
        String targetType,
        String sourceElementId,
        String sourceElementName,
        String targetPageId,
        String targetRoute,
        List<String> keywords,
        List<String> domainHints,
        String rationale,
        double confidenceScore
) {
    public CanonicalUiInteraction {
        interactionId = interactionId == null ? "" : interactionId.trim();
        pageId = pageId == null ? "" : pageId.trim();
        pageName = pageName == null ? "" : pageName.trim();
        canonicalName = canonicalName == null ? "" : canonicalName.trim();
        interactionType = interactionType == null ? "" : interactionType.trim();
        subjectType = subjectType == null ? "" : subjectType.trim();
        targetType = targetType == null ? "" : targetType.trim();
        sourceElementId = sourceElementId == null ? "" : sourceElementId.trim();
        sourceElementName = sourceElementName == null ? "" : sourceElementName.trim();
        targetPageId = targetPageId == null ? "" : targetPageId.trim();
        targetRoute = targetRoute == null ? "" : targetRoute.trim();
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        domainHints = domainHints == null ? List.of() : List.copyOf(domainHints);
        rationale = rationale == null ? "" : rationale.trim();
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
    }
}
