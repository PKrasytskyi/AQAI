package ua.demo.agentlab.persistence;

public record GeneratedSourceManifestEntry(
        GeneratedSourceKind kind,
        String packageName,
        String className,
        String relativePath,
        String contentHash
) {
    public GeneratedSourceManifestEntry {
        if (kind == null) {
            throw new IllegalArgumentException("kind cannot be null");
        }
        packageName = requireText(packageName, "packageName");
        className = requireText(className, "className");
        relativePath = requireText(relativePath, "relativePath");
        contentHash = requireText(contentHash, "contentHash");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
