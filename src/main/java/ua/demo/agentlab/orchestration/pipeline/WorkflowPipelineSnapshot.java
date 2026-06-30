package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.orchestration.WorkflowState;

public record WorkflowPipelineSnapshot(
        WorkflowRunEnvelope runEnvelope,
        RequirementPipelineStage requirements,
        DiscoveryPipelineStage discovery,
        MappingPipelineStage mapping,
        AiContextPipelineStage aiContext,
        GenerationPipelineStage generation
) {
    public static WorkflowPipelineSnapshot from(WorkflowState state) {
        if (state == null) {
            return new WorkflowPipelineSnapshot(
                    WorkflowRunEnvelope.from(null),
                    new RequirementPipelineStage(null, null, null),
                    new DiscoveryPipelineStage(null, null, null, null),
                    new MappingPipelineStage(null, null, null, null, null, null, null, null, null),
                    new AiContextPipelineStage(null),
                    new GenerationPipelineStage(null, null, null, null, null, null, null, null, null, null)
            );
        }
        return new WorkflowPipelineSnapshot(
                WorkflowRunEnvelope.from(state),
                new RequirementPipelineStage(
                        state.getRequirementDocument(),
                        state.getNormalizedRequirementBundle(),
                        state.getTestPlan()
                ),
                new DiscoveryPipelineStage(
                        state.getUiDiscoverySnapshot(),
                        state.getSeleniumDiscoveryResult(),
                        state.getPageModelBundle(),
                        state.getCanonicalPageFlowModel()
                ),
                new MappingPipelineStage(
                        state.getCanonicalTestCaseBundle(),
                        state.getUiTestPlan(),
                        state.getMappedUiKnowledge(),
                        state.getFlowScopedKnowledgePackage(),
                        state.getPageKnowledgeCacheLookupResult(),
                        state.getPageModelEnrichments(),
                        state.getEnrichedMappedUiKnowledge(),
                        state.getAssertionContracts(),
                        state.getKnowledgeRunMetadata()
                ),
                new AiContextPipelineStage(state.getAiContextPackage()),
                new GenerationPipelineStage(
                        state.getAiPageObjectSpecs(),
                        state.getAiUiTestSpecs(),
                        state.getPageObjectFiles(),
                        state.getUiTestFiles(),
                        state.getWrittenFiles(),
                        state.getAiArtifactFiles(),
                        state.getDiscoveryArtifactFiles(),
                        state.getGeneratedUiContractValidationResult(),
                        state.getGeneratedCodeValidationResult(),
                        state.getGeneratedCodeReviewReport()
                )
        );
    }
}
