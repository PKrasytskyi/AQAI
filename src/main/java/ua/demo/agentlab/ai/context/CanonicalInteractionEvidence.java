package ua.demo.agentlab.ai.context;

import java.util.List;

public record CanonicalInteractionEvidence(
        String pageId,
        String pageName,
        String pageType,
        String pageUrl,
        String pageUrlPattern,
        String pageTitle,
        String actionId,
        String actionName,
        String actionType,
        String description,
        String sourceElementId,
        String sourceElementName,
        String sourceElementType,
        String sourceElementRole,
        String sourceElementText,
        String targetPageId,
        List<String> supportedActions,
        List<String> locatorValues,
        List<String> extractedRoutes,
        List<String> tokens
) {
    public CanonicalInteractionEvidence {
        pageId = safe(pageId);
        pageName = safe(pageName);
        pageType = safe(pageType);
        pageUrl = safe(pageUrl);
        pageUrlPattern = safe(pageUrlPattern);
        pageTitle = safe(pageTitle);
        actionId = safe(actionId);
        actionName = safe(actionName);
        actionType = safe(actionType);
        description = safe(description);
        sourceElementId = safe(sourceElementId);
        sourceElementName = safe(sourceElementName);
        sourceElementType = safe(sourceElementType);
        sourceElementRole = safe(sourceElementRole);
        sourceElementText = safe(sourceElementText);
        targetPageId = safe(targetPageId);
        supportedActions = supportedActions == null ? List.of() : List.copyOf(supportedActions);
        locatorValues = locatorValues == null ? List.of() : List.copyOf(locatorValues);
        extractedRoutes = extractedRoutes == null ? List.of() : List.copyOf(extractedRoutes);
        tokens = tokens == null ? List.of() : List.copyOf(tokens);
    }

    public String searchableText() {
        return String.join(" ",
                pageName,
                pageType,
                pageUrl,
                pageUrlPattern,
                pageTitle,
                actionName,
                actionType,
                description,
                sourceElementName,
                sourceElementType,
                sourceElementRole,
                sourceElementText,
                targetPageId,
                String.join(" ", supportedActions),
                String.join(" ", locatorValues),
                String.join(" ", extractedRoutes),
                String.join(" ", tokens)
        ).trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
