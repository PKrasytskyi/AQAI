package ua.demo.agentlab.artifactreuse.model;

public record PomContractArtifactProvenance(
        String schemaVersion,
        String pageName,
        String artifactSource,
        String fingerprint,
        String originStablePath,
        String originArtifactId,
        String recordedByRunId
) {
    public PomContractArtifactProvenance {
        schemaVersion = clean(schemaVersion, "pom-contract-artifact-provenance.v1");
        pageName = clean(pageName, "");
        artifactSource = clean(artifactSource, "UNKNOWN");
        fingerprint = clean(fingerprint, "");
        originStablePath = clean(originStablePath, "");
        originArtifactId = clean(originArtifactId, "");
        recordedByRunId = clean(recordedByRunId, "");
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
