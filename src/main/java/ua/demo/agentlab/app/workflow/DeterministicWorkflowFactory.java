package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContractAgent;
import ua.demo.agentlab.ui.discovery.spa.agent.UiSpaSourceStateBindingAgent;
import ua.demo.agentlab.ui.discovery.spa.agent.UiSpaTargetStateBindingAgent;

import java.util.List;

public class DeterministicWorkflowFactory {

    public WorkflowDefinition create(WorkflowState initialState, WorkflowCoreComponents core) {
        if (core == null) {
            throw new IllegalArgumentException("core workflow components cannot be null");
        }
        List<WorkflowAgent> agents = List.of(
                core.requirementReaderAgent(),
                core.requirementNormalizationAgent(),
                new StructuredBehaviorContractAgent(),
                core.policyLoadingAgent(),
                core.uiDiscoveryAgent(),
                core.uiRuntimeEvidenceAgent(),
                core.uiPageModelAgent(),
                core.uiPageMappingAgent(),
                core.uiInteractionInventoryAgent(),
                core.uiSpaComponentInteractionGraphAgent(),
                core.uiPageKnowledgePersistenceAgent(),
                core.uiDiscoveryArtifactPersistenceAgent(),
                core.flowScopedKnowledgeAgent(),
                core.requirementToTestCaseAgent(),
                new UiSpaSourceStateBindingAgent(),
                core.uiSpaTargetedVerificationAgent(),
                core.uiLiveSpaTargetedVerificationAgent(),
                new UiSpaTargetStateBindingAgent(),
                core.uiSpaEvidenceRetentionAgent(),
                core.flowContractBuilderAgent(),
                core.flowContractPersistenceAgent(),
                core.flowSemanticIndexAgent(),
                core.flowSemanticCandidateAgent(),
                core.reusePlannerAgent(),
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
