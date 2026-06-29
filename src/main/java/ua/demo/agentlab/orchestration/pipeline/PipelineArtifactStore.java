package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class PipelineArtifactStore {

    private final WorkflowState state;
    private final Map<WorkflowArtifact, Object> artifacts = new LinkedHashMap<>();

    private PipelineArtifactStore(WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        this.state = state;
        loadFromState(state);
    }

    public static PipelineArtifactStore from(WorkflowState state) {
        return new PipelineArtifactStore(state);
    }

    public void refreshFromState() {
        artifacts.clear();
        loadFromState(state);
    }

    public Optional<Object> get(WorkflowArtifact artifact) {
        if (artifact == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(artifacts.get(artifact));
    }

    @SuppressWarnings("unchecked")
    public <T> T require(WorkflowArtifact artifact) {
        return (T) get(artifact)
                .orElseThrow(() -> new IllegalStateException("Missing pipeline artifact: " + artifact));
    }

    public void put(WorkflowArtifact artifact, Object value) {
        if (artifact == null || value == null) {
            return;
        }
        artifacts.put(artifact, value);
        if (artifact == WorkflowArtifact.FLOW_SCOPED_KNOWLEDGE_PACKAGE) {
            artifacts.put(WorkflowArtifact.REFRESHED_FLOW_SCOPED_KNOWLEDGE_PACKAGE, value);
        } else if (artifact == WorkflowArtifact.REFRESHED_FLOW_SCOPED_KNOWLEDGE_PACKAGE) {
            artifacts.put(WorkflowArtifact.FLOW_SCOPED_KNOWLEDGE_PACKAGE, value);
        }
        writeToState(artifact, value);
    }

    private void loadFromState(WorkflowState state) {
        putIfPresent(WorkflowArtifact.PROJECT_PROFILE, state.getProjectProfile());
        putIfPresent(WorkflowArtifact.REQUIREMENT_INPUT, state.getRequirementInput());
        putIfPresent(WorkflowArtifact.REQUIREMENT_DOCUMENT, state.getRequirementDocument());
        putIfPresent(WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE, state.getNormalizedRequirementBundle());
        putIfPresent(WorkflowArtifact.GENERATION_POLICY, state.getGenerationPolicy());
        putIfPresent(WorkflowArtifact.TEST_PLAN, state.getTestPlan());
        putIfPresent(WorkflowArtifact.UI_DISCOVERY_SNAPSHOT, state.getUiDiscoverySnapshot());
        putIfPresent(WorkflowArtifact.SELENIUM_DISCOVERY_RESULT, state.getSeleniumDiscoveryResult());
        putIfPresent(WorkflowArtifact.CANONICAL_PAGE_FLOW_MODEL, state.getCanonicalPageFlowModel());
        putIfPresent(WorkflowArtifact.PAGE_MODEL_BUNDLE, state.getPageModelBundle());
        putIfPresent(WorkflowArtifact.MAPPED_UI_KNOWLEDGE, state.getMappedUiKnowledge());
        putIfPresent(WorkflowArtifact.FLOW_SCOPED_KNOWLEDGE_PACKAGE, state.getFlowScopedKnowledgePackage());
        putIfPresent(WorkflowArtifact.REFRESHED_FLOW_SCOPED_KNOWLEDGE_PACKAGE, state.getFlowScopedKnowledgePackage());
        putIfPresent(WorkflowArtifact.PAGE_KNOWLEDGE_CACHE_LOOKUP, state.getPageKnowledgeCacheLookupResult());
        putIfPresent(WorkflowArtifact.PAGE_MODEL_ENRICHMENT_RECORDS, state.getPageModelEnrichments());
        putIfPresent(WorkflowArtifact.ENRICHED_MAPPED_UI_KNOWLEDGE, state.getEnrichedMappedUiKnowledge());
        putIfPresent(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE, state.getCanonicalTestCaseBundle());
        putIfPresent(WorkflowArtifact.UI_KNOWLEDGE_PERSISTED, state.getArtifacts().get("ui.knowledge.persistence.completed"));
        putIfPresent(WorkflowArtifact.ASSERTION_CONTRACTS, state.getAssertionContracts());
        putIfPresent(WorkflowArtifact.UI_TEST_PLAN, state.getUiTestPlan());
        putIfPresent(WorkflowArtifact.AI_CONTEXT_PACKAGE, state.getAiContextPackage());
        putIfPresent(WorkflowArtifact.AI_PAGE_OBJECT_SPECS, state.getAiPageObjectSpecs());
        putIfPresent(WorkflowArtifact.AI_UI_TEST_SPECS, state.getAiUiTestSpecs());
        putIfPresent(WorkflowArtifact.PAGE_OBJECT_FILES, state.getPageObjectFiles());
        putIfPresent(WorkflowArtifact.UI_TEST_FILES, state.getUiTestFiles());
        putIfPresent(WorkflowArtifact.GENERATED_UI_CONTRACT_VALIDATION, state.getGeneratedUiContractValidationResult());
        putIfPresent(WorkflowArtifact.GENERATED_CODE_VALIDATION, state.getGeneratedCodeValidationResult());
        putIfPresent(WorkflowArtifact.GENERATED_CODE_REVIEW, state.getGeneratedCodeReviewReport());
        putIfPresent(WorkflowArtifact.WRITTEN_FILES, state.getWrittenFiles());
    }

    private void putIfPresent(WorkflowArtifact artifact, Object value) {
        if (value != null) {
            artifacts.put(artifact, value);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void writeToState(WorkflowArtifact artifact, Object value) {
        switch (artifact) {
            case REQUIREMENT_DOCUMENT -> state.setRequirementDocument((ua.demo.agentlab.requirements.model.RequirementDocument) value);
            case NORMALIZED_REQUIREMENT_BUNDLE -> state.setNormalizedRequirementBundle((ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle) value);
            case GENERATION_POLICY -> state.setGenerationPolicy((ua.demo.agentlab.policy.model.GenerationPolicy) value);
            case TEST_PLAN -> state.setTestPlan((ua.demo.agentlab.futurefeat.testplan.model.TestPlan) value);
            case UI_DISCOVERY_SNAPSHOT -> {
                if (value instanceof ua.demo.agentlab.ui.discovery.agent.UiDiscoveryOutput output) {
                    state.setUiDiscoverySnapshot(output.discoverySnapshot());
                    state.setSeleniumDiscoveryResult(output.seleniumDiscoveryResult());
                    state.setCanonicalPageFlowModel(output.canonicalPageFlowModel());
                } else {
                    state.setUiDiscoverySnapshot((ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot) value);
                }
            }
            case SELENIUM_DISCOVERY_RESULT -> state.setSeleniumDiscoveryResult((ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult) value);
            case CANONICAL_PAGE_FLOW_MODEL -> state.setCanonicalPageFlowModel((ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel) value);
            case PAGE_MODEL_BUNDLE -> state.setPageModelBundle((ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle) value);
            case MAPPED_UI_KNOWLEDGE -> state.setMappedUiKnowledge((ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge) value);
            case FLOW_SCOPED_KNOWLEDGE_PACKAGE, REFRESHED_FLOW_SCOPED_KNOWLEDGE_PACKAGE -> state.setFlowScopedKnowledgePackage((ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage) value);
            case PAGE_KNOWLEDGE_CACHE_LOOKUP -> {
                if (value instanceof ua.demo.agentlab.ai.pageenrichment.agent.PageKnowledgeCacheLookupOutput output) {
                    state.setKnowledgeRunMetadata(output.runMetadata());
                    state.setPageKnowledgeCacheLookupResult(output.result());
                } else {
                    state.setPageKnowledgeCacheLookupResult((ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheLookupResult) value);
                }
            }
            case PAGE_MODEL_ENRICHMENT_RECORDS -> state.setPageModelEnrichments((java.util.List) value);
            case PAGE_MODEL_ENRICHMENT_OUTPUT -> {
                if (value instanceof ua.demo.agentlab.ai.pageenrichment.agent.PageModelEnrichmentOutput output) {
                    state.setPageModelEnrichments(output.records());
                    state.setEnrichedMappedUiKnowledge(output.enrichedMappedUiKnowledge());
                }
            }
            case ENRICHED_MAPPED_UI_KNOWLEDGE -> state.setEnrichedMappedUiKnowledge((ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge) value);
            case CANONICAL_TEST_CASE_BUNDLE -> state.setCanonicalTestCaseBundle((ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle) value);
            case TEST_CASE_EXPECTATION_ENRICHMENT -> {
                if (value instanceof ua.demo.agentlab.ai.expectationenrichment.agent.TestCaseExpectationEnrichmentOutput output) {
                    state.setCanonicalTestCaseBundle(output.bundle());
                }
            }
            case UI_KNOWLEDGE_PERSISTED -> {
                if (value instanceof ua.demo.agentlab.ui.discovery.agent.UiKnowledgePersistenceOutput output) {
                    state.setKnowledgeRunMetadata(output.runMetadata());
                    state.addArtifact("ui.knowledge.persistence.completed", "true");
                } else {
                    state.addArtifact("ui.knowledge.persistence.completed", String.valueOf(value));
                }
            }
            case ASSERTION_CONTRACTS -> state.setAssertionContracts((java.util.List) value);
            case UI_TEST_PLAN -> state.setUiTestPlan((ua.demo.agentlab.ui.UiTestPlan) value);
            case AI_CONTEXT_PACKAGE -> state.setAiContextPackage((ua.demo.agentlab.ai.context.AiContextPackage) value);
            case AI_PAGE_OBJECT_SPECS -> {
                if (value instanceof ua.demo.agentlab.ai.ui.generation.AiPageObjectGenerationResult result) {
                    state.setAiPageObjectSpecs(result.specs());
                } else {
                    state.setAiPageObjectSpecs((java.util.List) value);
                }
            }
            case AI_UI_TEST_SPECS -> state.setAiUiTestSpecs((java.util.List) value);
            case PAGE_OBJECT_FILES -> state.setPageObjectFiles((java.util.List) value);
            case UI_TEST_FILES -> state.setUiTestFiles((java.util.List) value);
            case GENERATED_UI_CONTRACT_VALIDATION -> state.setGeneratedUiContractValidationResult((ua.demo.agentlab.validation.GeneratedUiContractValidationResult) value);
            case GENERATED_CODE_VALIDATION -> state.setGeneratedCodeValidationResult((ua.demo.agentlab.validation.GeneratedCodeValidationResult) value);
            case GENERATED_CODE_REVIEW -> state.setGeneratedCodeReviewReport((ua.demo.agentlab.review.GeneratedCodeReviewReport) value);
            default -> {
            }
        }
    }
}
