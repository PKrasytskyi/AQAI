package ua.demo.agentlab.artifactreuse.model;

public record ArtifactTarget(
        ArtifactTargetType targetType,
        String targetId,
        String pageId,
        String pageName,
        String route,
        String capability
) {
    public ArtifactTarget {
        targetType = targetType == null ? ArtifactTargetType.UNKNOWN : targetType;
        targetId = safe(targetId);
        pageId = safe(pageId);
        pageName = safe(pageName);
        route = safe(route);
        capability = safe(capability);
    }

    public static ArtifactTarget page(String pageId, String pageName, String route, String capability) {
        return new ArtifactTarget(ArtifactTargetType.PAGE, pageId, pageId, pageName, route, capability);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
