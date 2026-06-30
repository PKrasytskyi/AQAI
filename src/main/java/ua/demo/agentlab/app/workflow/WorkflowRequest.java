package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.model.SourceType;

public record WorkflowRequest(
        String requirementLocation,
        SourceType sourceType,
        WorkflowMode mode,
        ProjectProfile projectProfile
) {
    public WorkflowRequest {
        requirementLocation = requirementLocation == null ? "" : requirementLocation.trim();
        sourceType = sourceType == null ? SourceType.FILE : sourceType;
        mode = mode == null ? WorkflowMode.DETERMINISTIC : mode;
        if (projectProfile == null) {
            throw new IllegalArgumentException("projectProfile cannot be null");
        }
    }
}
