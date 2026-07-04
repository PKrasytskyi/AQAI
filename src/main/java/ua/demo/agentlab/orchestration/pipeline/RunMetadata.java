package ua.demo.agentlab.orchestration.pipeline;

import java.time.Instant;
import java.util.UUID;

public class RunMetadata {

    private final String runId;
    private final Instant createdAt;
    private boolean failed;
    private String failureReason;

    public RunMetadata() {
        this(UUID.randomUUID().toString(), Instant.now(), false, "");
    }

    private RunMetadata(String runId, Instant createdAt, boolean failed, String failureReason) {
        this.runId = runId == null || runId.isBlank() ? UUID.randomUUID().toString() : runId;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.failed = failed;
        this.failureReason = failureReason == null ? "" : failureReason.trim();
    }

    public static RunMetadata from(boolean failed, String failureReason) {
        return new RunMetadata(UUID.randomUUID().toString(), Instant.now(), failed, failureReason);
    }

    public String runId() {
        return runId;
    }

    public String getRunId() {
        return runId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String getCreatedAt() {
        return createdAt.toString();
    }

    public boolean failed() {
        return failed;
    }

    public boolean isFailed() {
        return failed;
    }

    public String failureReason() {
        return failureReason;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void fail(String reason) {
        failed = true;
        failureReason = reason == null ? "" : reason.trim();
    }

    public RunMetadata snapshot() {
        return new RunMetadata(runId, createdAt, failed, failureReason);
    }
}
