package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;

import java.util.List;

public class AiPromptWorkflowFactory {

    private final AiPromptModuleFactory aiPromptModuleFactory;

    public AiPromptWorkflowFactory() {
        this(new AiPromptModuleFactory());
    }

    AiPromptWorkflowFactory(AiPromptModuleFactory aiPromptModuleFactory) {
        if (aiPromptModuleFactory == null) {
            throw new IllegalArgumentException("ai prompt module factory cannot be null");
        }
        this.aiPromptModuleFactory = aiPromptModuleFactory;
    }

    public WorkflowDefinition create(WorkflowState initialState, WorkflowCoreComponents core) {
        if (core == null) {
            throw new IllegalArgumentException("core workflow components cannot be null");
        }
        AiPromptModule ai = aiPromptModuleFactory.create(core);
        List<WorkflowAgent> agents = List.of(
                core.requirementReaderAgent(),
                core.requirementNormalizationAgent(),
                core.policyLoadingAgent(),
                core.uiDiscoveryAgent(),
                core.uiRuntimeEvidenceAgent(),
                core.uiPageModelAgent(),
                core.uiPageMappingAgent(),
                core.uiPageKnowledgePersistenceAgent(),
                core.uiDiscoveryArtifactPersistenceAgent(),
                ai.flowScopedKnowledgeAgent(),
                core.requirementToTestCaseAgent(),
                ai.testCaseExpectationEnrichmentAgent(),
                ai.assertionContractAgent(),
                core.uiTestPlanAgent(),
                ai.pageKnowledgeCacheLookupAgent(),
                ai.pageModelEnrichmentAgent(),
                ai.flowScopedKnowledgeRefreshAgent(),
                ai.aiContextAssemblyAgent(),
                ai.aiPageObjectSpecAgent(),
                core.filePersistenceAgent()
        );
        return new WorkflowDefinition(initialState, agents);
    }
}
