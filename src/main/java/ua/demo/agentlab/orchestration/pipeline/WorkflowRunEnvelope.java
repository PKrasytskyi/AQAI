package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.model.RequirementInput;

import java.util.List;
import java.util.Map;

public record WorkflowRunEnvelope(
        String objective,
        RequirementInput requirementInput,
        Map<String, String> artifacts,
        List<String> findings,
        List<String> auditTrail,
        boolean failed,
        String failureReason
) {
    public WorkflowRunEnvelope {
        objective = objective == null ? "" : objective.trim();
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
        findings = findings == null ? List.of() : List.copyOf(findings);
        auditTrail = auditTrail == null ? List.of() : List.copyOf(auditTrail);
        failureReason = failureReason == null ? "" : failureReason.trim();
    }

    public static WorkflowRunEnvelope from(WorkflowState state) {
        if (state == null) {
            return new WorkflowRunEnvelope("", null, Map.of(), List.of(), List.of(), false, "");
        }
        return new WorkflowRunEnvelope(
                state.getObjective(),
                state.getRequirementInput(),
                state.getArtifacts(),
                state.getFindings(),
                state.getAuditTrail(),
                state.isFailed(),
                state.getFailureReason()
        );
    }
}
