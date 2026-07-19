package ua.demo.agentlab.ui.discovery.spa.model;

public enum SpaEvidenceStatus {
    CANDIDATE,
    LIVE_VERIFIED,
    STABLE,
    PROMPT_ALLOWED,
    /** Legacy persisted status; new writes use the explicit lifecycle states above. */
    CONFIRMED,
    DEGRADED,
    NEEDS_REVIEW
}
