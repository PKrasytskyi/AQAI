package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.ai.context.RuleBasedCanonicalInteractionLayer;
import ua.demo.agentlab.orchestration.WorkflowAgent;

public record KnowledgeStoreModule(
        RuleBasedCanonicalInteractionLayer canonicalInteractionLayer,
        WorkflowAgent uiPageKnowledgePersistenceAgent,
        WorkflowAgent flowScopedKnowledgeAgent,
        WorkflowAgent flowContractBuilderAgent,
        WorkflowAgent flowContractPersistenceAgent,
        WorkflowAgent flowRuntimeFeedbackAgent,
        WorkflowAgent flowSemanticIndexAgent,
        WorkflowAgent flowSemanticCandidateAgent,
        WorkflowAgent reusePlannerAgent
) {
}
