package ua.demo.agentlab.artifactreuse.policy;

public enum ArtifactReuseDecisionType {
    FORCE_REFRESH,
    REUSE_STABLE,
    CALL_LLM,
    REGENERATE_SCHEMA_CHANGED,
    REGENERATE_PREVIOUS_INVALID
}
