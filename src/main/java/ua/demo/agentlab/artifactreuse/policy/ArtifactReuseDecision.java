package ua.demo.agentlab.artifactreuse.policy;

public record ArtifactReuseDecision(
        ArtifactReuseDecisionType type,
        String reason
) {
    public ArtifactReuseDecision {
        type = type == null ? ArtifactReuseDecisionType.CALL_LLM : type;
        reason = reason == null ? "" : reason.trim();
    }

    public boolean reuseStable() {
        return type == ArtifactReuseDecisionType.REUSE_STABLE;
    }

    public boolean callLlm() {
        return type != ArtifactReuseDecisionType.REUSE_STABLE;
    }
}
