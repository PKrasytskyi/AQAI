package ua.demo.agentlab.artifactreuse.model;

public enum ArtifactStatus {
    GENERATED,
    SCHEMA_VALIDATED,
    QUALITY_VALIDATED,
    WRITER_VALIDATED,
    COMPILE_VALIDATED,
    SMOKE_VALIDATED,
    STABLE,
    STALE,
    INVALIDATED,
    NEEDS_REVIEW
}
