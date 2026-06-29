package ua.demo.agentlab.ai.artifactdiff;

public record ArtifactCategoryDiff(
        String category,
        String status,
        String currentPath,
        String previousPath,
        String currentHash,
        String previousHash,
        String note
) {
    public ArtifactCategoryDiff {
        category = safe(category);
        status = safe(status);
        currentPath = safe(currentPath);
        previousPath = safe(previousPath);
        currentHash = safe(currentHash);
        previousHash = safe(previousHash);
        note = safe(note);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
