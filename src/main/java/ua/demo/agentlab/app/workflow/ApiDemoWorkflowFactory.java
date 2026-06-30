package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;

import java.util.List;

public class ApiDemoWorkflowFactory {

    public WorkflowDefinition create(WorkflowState initialState, WorkflowCoreComponents core) {
        if (core == null) {
            throw new IllegalArgumentException("core workflow components cannot be null");
        }
        List<WorkflowAgent> agents = List.of(
                core.apiGenerationAgent(),
                core.apiGeneratedSourcePersistenceAgent(),
                core.generatedCodeCompileAgent()
        );
        return new WorkflowDefinition(initialState, agents);
    }
}
