package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class PipelineArtifactStore {

    private final Map<WorkflowArtifact, Object> artifacts = new LinkedHashMap<>();

    private PipelineArtifactStore(WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        loadFromState(state);
    }

    public static PipelineArtifactStore from(WorkflowState state) {
        return new PipelineArtifactStore(state);
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
        expandCompoundOutput(artifact, value);
        if (artifact == WorkflowArtifact.FLOW_SCOPED_KNOWLEDGE_PACKAGE) {
            artifacts.put(WorkflowArtifact.REFRESHED_FLOW_SCOPED_KNOWLEDGE_PACKAGE, value);
        } else if (artifact == WorkflowArtifact.REFRESHED_FLOW_SCOPED_KNOWLEDGE_PACKAGE) {
            artifacts.put(WorkflowArtifact.FLOW_SCOPED_KNOWLEDGE_PACKAGE, value);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void expandCompoundOutput(WorkflowArtifact artifact, Object value) {
        if (value instanceof ua.demo.agentlab.ui.discovery.agent.UiDiscoveryOutput output) {
            putIfPresent(WorkflowArtifact.UI_DISCOVERY_SNAPSHOT, output.discoverySnapshot());
            putIfPresent(WorkflowArtifact.SELENIUM_DISCOVERY_RESULT, output.seleniumDiscoveryResult());
            putIfPresent(WorkflowArtifact.CANONICAL_PAGE_FLOW_MODEL, output.canonicalPageFlowModel());
            return;
        }
        if (value instanceof ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle output) {
            putIfPresent(WorkflowArtifact.UI_RUNTIME_EVIDENCE, output);
            return;
        }
        if (value instanceof ua.demo.agentlab.ui.discovery.agent.UiPageMappingOutput output) {
            putIfPresent(WorkflowArtifact.MAPPED_UI_KNOWLEDGE, output.curatedKnowledge().knowledge());
            return;
        }
        if (value instanceof ua.demo.agentlab.ai.pageenrichment.agent.PageKnowledgeCacheLookupOutput output) {
            putIfPresent(WorkflowArtifact.PAGE_KNOWLEDGE_CACHE_LOOKUP, output.result());
            return;
        }
        if (value instanceof ua.demo.agentlab.ai.pageenrichment.agent.PageModelEnrichmentOutput output) {
            putIfPresent(WorkflowArtifact.PAGE_MODEL_ENRICHMENT_RECORDS, output.records());
            putIfPresent(WorkflowArtifact.ENRICHED_MAPPED_UI_KNOWLEDGE, output.enrichedMappedUiKnowledge());
            return;
        }
        if (value instanceof ua.demo.agentlab.ai.expectationenrichment.agent.TestCaseExpectationEnrichmentOutput output) {
            putIfPresent(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE, output.bundle());
            return;
        }
        if (value instanceof ua.demo.agentlab.ai.context.AiContextPackage contextPackage) {
            putIfPresent(WorkflowArtifact.AI_CONTEXT_PACKAGE, contextPackage);
            return;
        }
        if (value instanceof ua.demo.agentlab.ai.ui.generation.AiPageObjectGenerationResult result) {
            putIfPresent(WorkflowArtifact.AI_PAGE_OBJECT_SPECS, result.specs());
            return;
        }
        if (value instanceof ua.demo.agentlab.ai.ui.generation.AiUiTestGenerationResult result) {
            putIfPresent(WorkflowArtifact.AI_UI_TEST_SPECS, result.specs());
            return;
        }
        if (value instanceof java.util.List list) {
            if (artifact == WorkflowArtifact.PAGE_OBJECT_FILES) {
                putIfPresent(WorkflowArtifact.PAGE_OBJECT_FILES, list);
            } else if (artifact == WorkflowArtifact.UI_TEST_FILES) {
                putIfPresent(WorkflowArtifact.UI_TEST_FILES, list);
            } else if (artifact == WorkflowArtifact.WRITTEN_FILES) {
                putIfPresent(WorkflowArtifact.WRITTEN_FILES, list);
            }
        }
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
        putIfPresent(WorkflowArtifact.UI_RUNTIME_EVIDENCE, state.getRuntimeEvidenceBundle());
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

}
