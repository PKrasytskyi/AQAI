package ua.demo.agentlab.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public record GenerationNamespace(
        String schemaVersion,
        String namespaceId,
        String profileId,
        String baseUrlHash,
        String pagesPackage,
        String testsPackage
) {

    public static final String SCHEMA_VERSION = "generation-namespace.v1";
    private static final String DEFAULT_PACKAGE_ROOT = "ua.demo.agentlab.ui.generated";

    public GenerationNamespace {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        namespaceId = requireText(namespaceId, "namespaceId");
        profileId = requireText(profileId, "profileId");
        baseUrlHash = requireText(baseUrlHash, "baseUrlHash");
        pagesPackage = requireText(pagesPackage, "pagesPackage");
        testsPackage = requireText(testsPackage, "testsPackage");
    }

    public static GenerationNamespace resolve(
            String profileId,
            String baseUrl,
            String configuredPagesPackage,
            String configuredTestsPackage
    ) {
        String safeProfileId = requireText(profileId, "profileId");
        String baseUrlHash = sha256(requireText(baseUrl, "baseUrl"));
        String namespaceId = packageSegment(safeProfileId) + "_" + baseUrlHash.substring(0, 8);
        String defaultPrefix = DEFAULT_PACKAGE_ROOT + "." + namespaceId;
        return new GenerationNamespace(
                SCHEMA_VERSION,
                namespaceId,
                safeProfileId,
                baseUrlHash,
                firstNonBlank(configuredPagesPackage, defaultPrefix + ".pages"),
                firstNonBlank(configuredTestsPackage, defaultPrefix + ".tests")
        );
    }

    private static String packageSegment(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            return "project";
        }
        return Character.isDigit(normalized.charAt(0)) ? "p_" + normalized : normalized;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte current : digest) {
                result.append(String.format("%02x", current));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot calculate generation namespace hash", exception);
        }
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
