package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.policy.provider.DefaultGenerationPolicyProvider;

public record RequirementPolicyModule(
        DefaultGenerationPolicyProvider policyProvider,
        GenerationPolicy defaultPolicy,
        WorkflowAgent requirementReaderAgent,
        WorkflowAgent requirementNormalizationAgent,
        WorkflowAgent policyLoadingAgent
) {
}
