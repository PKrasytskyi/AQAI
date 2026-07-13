package ua.demo.agentlab.artifactreuse.policy;

public record ArtifactReusePolicyInput(
        boolean reuseEnabled,
        boolean forceRefresh,
        boolean stableArtifactFound,
        boolean stableArtifactReadable,
        String artifactStatus,
        String requestedSchemaVersion,
        String existingSchemaVersion,
        ArtifactInvalidationInput invalidationInput
) {
    public ArtifactReusePolicyInput(
            boolean reuseEnabled,
            boolean forceRefresh,
            boolean stableArtifactFound,
            boolean stableArtifactReadable,
            String artifactStatus,
            String requestedSchemaVersion,
            String existingSchemaVersion
    ) {
        this(reuseEnabled, forceRefresh, stableArtifactFound, stableArtifactReadable, artifactStatus,
                requestedSchemaVersion, existingSchemaVersion,
                new ArtifactInvalidationInput(reuseEnabled, forceRefresh, true,
                        same(requestedSchemaVersion, existingSchemaVersion), true, true,
                        true, true, true, true, true, true));
    }

    public ArtifactReusePolicyInput {
        artifactStatus = safe(artifactStatus);
        requestedSchemaVersion = safe(requestedSchemaVersion);
        existingSchemaVersion = safe(existingSchemaVersion);
        invalidationInput = invalidationInput == null
                ? new ArtifactInvalidationInput(reuseEnabled, forceRefresh, true,
                same(requestedSchemaVersion, existingSchemaVersion), true, true,
                true, true, true, true, true, true)
                : invalidationInput;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean same(String requested, String existing) {
        return existing == null || existing.isBlank() || requested == null || requested.isBlank()
                || requested.trim().equals(existing.trim());
    }
}
