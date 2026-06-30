package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;

import java.util.List;

public record WorkflowDefinition(
        WorkflowState initialState,
        List<WorkflowAgent> agents
) {
    public WorkflowDefinition {
        if (initialState == null) {
            throw new IllegalArgumentException("initialState cannot be null");
        }
        agents = agents == null ? List.of() : List.copyOf(agents);
    }
}
