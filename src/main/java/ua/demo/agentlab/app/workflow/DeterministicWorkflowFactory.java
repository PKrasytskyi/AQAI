package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;

import java.util.List;

public class DeterministicWorkflowFactory {

    public WorkflowDefinition create(WorkflowState initialState, WorkflowCoreComponents core) {
        if (core == null) {
            throw new IllegalArgumentException("core workflow components cannot be null");
        }
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
                core.flowScopedKnowledgeAgent(),
                core.requirementToTestCaseAgent(),
                core.uiTestPlanAgent(),
                core.pageObjectWriterAgent(),
                core.layeredUiTestWriterAgent(),
                core.filePersistenceAgent(),
                core.generatedUiContractValidationAgent(),
                core.generatedCodeCompileAgent(),
                core.generatedCodeReviewAgent()
        );
        return new WorkflowDefinition(initialState, agents);
    }
}
