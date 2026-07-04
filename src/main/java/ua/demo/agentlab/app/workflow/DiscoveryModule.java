package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;

public record DiscoveryModule(
        WorkflowAgent uiDiscoveryAgent,
        WorkflowAgent uiRuntimeEvidenceAgent,
        WorkflowAgent uiDiscoveryArtifactPersistenceAgent,
        WorkflowAgent uiPageModelAgent,
        WorkflowAgent uiPageMappingAgent
) {
}
