package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;

public record ApiModule(
        WorkflowAgent apiGenerationAgent,
        WorkflowAgent apiGeneratedSourcePersistenceAgent
) {
}
