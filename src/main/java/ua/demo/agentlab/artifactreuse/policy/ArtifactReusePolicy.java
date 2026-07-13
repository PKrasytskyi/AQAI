package ua.demo.agentlab.artifactreuse.policy;

public class ArtifactReusePolicy {

    private final ArtifactInvalidationPolicy invalidationPolicy;

    public ArtifactReusePolicy() {
        this(new ArtifactInvalidationPolicy());
    }

    ArtifactReusePolicy(ArtifactInvalidationPolicy invalidationPolicy) {
        if (invalidationPolicy == null) {
            throw new IllegalArgumentException("invalidation policy cannot be null");
        }
        this.invalidationPolicy = invalidationPolicy;
    }

    public ArtifactReuseDecision decide(ArtifactReusePolicyInput input) {
        if (input == null) {
            return new ArtifactReuseDecision(ArtifactReuseDecisionType.CALL_LLM, "reuse input is missing");
        }
        ArtifactInvalidationDecision invalidation = invalidationPolicy.evaluate(input.invalidationInput());
        if (!input.reuseEnabled()) return new ArtifactReuseDecision(ArtifactReuseDecisionType.CALL_LLM, "artifact reuse is disabled");
        if (input.forceRefresh()) return new ArtifactReuseDecision(ArtifactReuseDecisionType.FORCE_REFRESH, "force refresh requested");
        if (!input.stableArtifactFound()) {
            return new ArtifactReuseDecision(ArtifactReuseDecisionType.CALL_LLM, "no stable artifact found");
        }
        if (!input.stableArtifactReadable()) {
            return new ArtifactReuseDecision(ArtifactReuseDecisionType.REGENERATE_PREVIOUS_INVALID,
                    "stable artifact exists but cannot be read");
        }
        if (!sameSchema(input.requestedSchemaVersion(), input.existingSchemaVersion())) {
            return new ArtifactReuseDecision(ArtifactReuseDecisionType.REGENERATE_SCHEMA_CHANGED,
                    "artifact schema changed");
        }
        if (!stableStatus(input.artifactStatus())) {
            return new ArtifactReuseDecision(ArtifactReuseDecisionType.REGENERATE_PREVIOUS_INVALID,
                    "artifact status is not reusable: " + input.artifactStatus());
        }
        if (!invalidation.reusable()) {
            return new ArtifactReuseDecision(ArtifactReuseDecisionType.REGENERATE_PREVIOUS_INVALID,
                    "artifact invalidated: " + invalidation.reasons());
        }
        return new ArtifactReuseDecision(ArtifactReuseDecisionType.REUSE_STABLE, "stable artifact can be reused");
    }

    private boolean sameSchema(String requested, String existing) {
        return existing == null || existing.isBlank() || requested == null || requested.isBlank()
                || requested.trim().equals(existing.trim());
    }

    private boolean stableStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return "STABLE".equals(status.trim().toUpperCase());
    }
}
