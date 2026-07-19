package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.discovery.spa.agent.UiSpaSourceStateBindingAgent;
import ua.demo.agentlab.ui.discovery.spa.agent.UiSpaTargetStateBindingAgent;
import ua.demo.agentlab.ui.discovery.interaction.agent.UiInteractionEvidenceAgent;
import ua.demo.agentlab.demo.BuildWeekDemoCompletionAgent;

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
                ai.structuredBehaviorContractAgent(),
                core.policyLoadingAgent(),
                core.uiDiscoveryAgent(),
                core.uiRuntimeEvidenceAgent(),
                core.uiPageModelAgent(),
                core.uiPageMappingAgent(),
                core.uiInteractionInventoryAgent(),
                core.uiSpaComponentInteractionGraphAgent(),
                core.uiPageKnowledgePersistenceAgent(),
                core.uiDiscoveryArtifactPersistenceAgent(),
                ai.flowScopedKnowledgeAgent(),
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
                ai.testCaseExpectationEnrichmentAgent(),
                ai.assertionContractAgent(),
                core.uiTestPlanAgent(),
                ai.pageKnowledgeCacheLookupAgent(),
                ai.pageModelEnrichmentAgent(),
                ai.flowScopedKnowledgeRefreshAgent(),
                ai.aiContextAssemblyAgent(),
                new UiInteractionEvidenceAgent(),
                ai.uiEvidenceFunnelAgent(),
                ai.aiPageObjectSpecAgent(),
                ai.pomContractPageObjectWriterAgent(),
                ai.uiTestContractAgent(),
                ai.uiTestContractValidationAgent(),
                ai.deterministicTestNgWriterAgent(),
                core.filePersistenceAgent(),
                core.generatedUiContractValidationAgent(),
                core.generatedCodeCompileAgent(),
                core.generatedCodeReviewAgent(),
                core.generatedUiSmokeAgent(),
                core.generatedTestExecutionAgent(),
                core.uiSpaSmokeEvidenceFeedbackAgent(),
                core.artifactLifecyclePromotionAgent(),
                core.flowRuntimeFeedbackAgent(),
                core.artifactReuseMetricsAgent(),
                core.runtimeFeedbackDbUpdateAgent(),
                core.runHistoryStatisticsAgent(),
                new BuildWeekDemoCompletionAgent()
        );
        return new WorkflowDefinition(initialState, agents);
    }
}
