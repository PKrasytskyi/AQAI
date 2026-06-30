package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;

public record AiPromptModule(
        WorkflowAgent flowScopedKnowledgeAgent,
        WorkflowAgent testCaseExpectationEnrichmentAgent,
        WorkflowAgent assertionContractAgent,
        WorkflowAgent pageKnowledgeCacheLookupAgent,
        WorkflowAgent pageModelEnrichmentAgent,
        WorkflowAgent flowScopedKnowledgeRefreshAgent,
        WorkflowAgent aiContextAssemblyAgent,
        WorkflowAgent aiPageObjectSpecAgent
) {
}
