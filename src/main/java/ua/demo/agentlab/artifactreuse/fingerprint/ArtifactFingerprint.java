package ua.demo.agentlab.artifactreuse.fingerprint;

import java.util.Map;

public record ArtifactFingerprint(
        String algorithm,
        String value,
        String canonicalSource
) {
    public ArtifactFingerprint {
        algorithm = safe(algorithm).isBlank() ? "SHA-256" : safe(algorithm);
        value = safe(value);
        canonicalSource = safe(canonicalSource);
    }

    public boolean present() {
        return !value.isBlank();
    }

    public Map<String, String> asArtifactMap() {
        return Map.of(
                "algorithm", algorithm,
                "value", value
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
