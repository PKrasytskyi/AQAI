package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.model.RequirementInput;

import java.util.List;
import java.util.Map;

public record WorkflowRunEnvelope(
        String objective,
        RequirementInput requirementInput,
        RunMetadata runMetadata,
        AuditTrail audit,
        Findings findings,
        ArtifactRefs artifactRefs
) {
    public WorkflowRunEnvelope {
        objective = objective == null ? "" : objective.trim();
        runMetadata = runMetadata == null ? new RunMetadata() : runMetadata;
        audit = audit == null ? new AuditTrail() : audit;
        findings = findings == null ? new Findings() : findings;
        artifactRefs = artifactRefs == null ? new ArtifactRefs() : artifactRefs;
    }

    public WorkflowRunEnvelope(
            String objective,
            RequirementInput requirementInput,
            Map<String, String> artifacts,
            List<String> findings,
            List<String> auditTrail,
            boolean failed,
            String failureReason
    ) {
        this(
                objective,
                requirementInput,
                RunMetadata.from(failed, failureReason),
                AuditTrail.from(auditTrail),
                Findings.from(findings),
                ArtifactRefs.from(artifacts)
        );
    }

    public static WorkflowRunEnvelope create(String objective, RequirementInput requirementInput) {
        return new WorkflowRunEnvelope(
                objective,
                requirementInput,
                new RunMetadata(),
                new AuditTrail(),
                new Findings(),
                new ArtifactRefs()
        );
    }

    public static WorkflowRunEnvelope from(WorkflowState state) {
        if (state == null) {
            return create("", null);
        }
        WorkflowRunEnvelope envelope = state.runEnvelope();
        if (envelope == null) {
            return create("", null);
        }
        return new WorkflowRunEnvelope(
                envelope.objective(),
                envelope.requirementInput(),
                envelope.runMetadata().snapshot(),
                envelope.audit().snapshot(),
                envelope.findings().snapshot(),
                envelope.artifactRefs().snapshot()
        );
    }

    public Map<String, String> artifacts() {
        return Map.copyOf(artifactRefs.asMap());
    }

    public List<String> findingsList() {
        return List.copyOf(findings.entries());
    }

    public List<String> auditTrail() {
        return List.copyOf(audit.entries());
    }

    public boolean failed() {
        return runMetadata.failed();
    }

    public String failureReason() {
        return runMetadata.failureReason();
    }
}
