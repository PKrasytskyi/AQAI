package ua.demo.agentlab.artifactreuse.flow;

import java.time.Instant;

public record FlowRuntimeFeedback(boolean smokePassed, String smokeSource, String occurredAt) {
    public FlowRuntimeFeedback {
        smokeSource = smokeSource == null || smokeSource.isBlank() ? "generated-ui-smoke" : smokeSource.trim();
        occurredAt = occurredAt == null || occurredAt.isBlank() ? Instant.now().toString() : occurredAt.trim();
    }
}
