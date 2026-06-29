package ua.demo.agentlab.ui.discovery.persistence.knowledge;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record KnowledgeRunMetadata(
        String runId,
        String appId,
        String baseUrlHash,
        String requirementSetHash,
        String discoverySessionId,
        String schemaVersion,
        String createdAt,
        String sourceAgent,
        double confidence
) {
    public static final String CURRENT_SCHEMA_VERSION = "ui-knowledge-v2";

    public KnowledgeRunMetadata {
        runId = safe(runId);
        appId = safe(appId);
        baseUrlHash = safe(baseUrlHash);
        requirementSetHash = safe(requirementSetHash);
        discoverySessionId = safe(discoverySessionId);
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? CURRENT_SCHEMA_VERSION : schemaVersion.trim();
        createdAt = createdAt == null || createdAt.isBlank() ? Instant.now().toString() : createdAt.trim();
        sourceAgent = safe(sourceAgent);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    public static KnowledgeRunMetadata from(WorkflowState state, String sourceAgent) {
        String appId = state.getProjectProfile() == null ? "unknown-app" : state.getProjectProfile().profileId();
        String baseUrl = state.getProjectProfile() == null ? "" : state.getProjectProfile().baseUrl();
        String requirementText = state.getNormalizedRequirementBundle() == null
                ? state.getRequirementInput().location()
                : state.getNormalizedRequirementBundle().requirements().stream()
                .map(NormalizedRequirement::id)
                .sorted()
                .collect(Collectors.joining("|"));
        String discoverySeed = state.getSeleniumDiscoveryResult() == null
                ? requirementText
                : state.getSeleniumDiscoveryResult().pages().stream()
                .map(page -> page.pageId() + ":" + page.fingerprint())
                .sorted()
                .collect(Collectors.joining("|"));
        String baseUrlHash = sha256(baseUrl);
        String requirementSetHash = sha256(requirementText);
        String runId = UUID.nameUUIDFromBytes((appId + "|" + baseUrlHash + "|" + requirementSetHash
                + "|" + Instant.now()).getBytes(StandardCharsets.UTF_8)).toString();
        return new KnowledgeRunMetadata(
                runId,
                appId,
                baseUrlHash,
                requirementSetHash,
                sha256(discoverySeed),
                CURRENT_SCHEMA_VERSION,
                Instant.now().toString(),
                sourceAgent,
                1.0d
        );
    }

    public Map<String, String> asMap() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("runId", runId);
        metadata.put("appId", appId);
        metadata.put("baseUrlHash", baseUrlHash);
        metadata.put("requirementSetHash", requirementSetHash);
        metadata.put("discoverySessionId", discoverySessionId);
        metadata.put("schemaVersion", schemaVersion);
        metadata.put("createdAt", createdAt);
        metadata.put("sourceAgent", sourceAgent);
        metadata.put("confidence", String.valueOf(confidence));
        return Map.copyOf(metadata);
    }

    public Map<String, String> namespaceFilter() {
        return new KnowledgeNamespaceFilterBuilder().currentRunOnly(this);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(safe(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception exception) {
            return Integer.toHexString(safe(value).hashCode());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
