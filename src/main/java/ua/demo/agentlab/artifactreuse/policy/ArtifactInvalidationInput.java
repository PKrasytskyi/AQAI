package ua.demo.agentlab.artifactreuse.policy;

public record ArtifactInvalidationInput(
        boolean reuseEnabled,
        boolean forceRefresh,
        boolean fingerprintMatches,
        boolean schemaMatches,
        boolean promptTemplateMatches,
        boolean writerVersionMatches,
        boolean qualityGatePassed,
        boolean compilePassed,
        boolean smokePassed,
        boolean pageFingerprintMatches,
        boolean requiredActionsMatch,
        boolean requiredAssertionsMatch
) {
}
