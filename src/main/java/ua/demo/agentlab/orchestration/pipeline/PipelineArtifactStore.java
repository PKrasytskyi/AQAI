package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;

import java.util.LinkedHashMap;
import java.util.List;
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

    public <T> List<T> getList(WorkflowArtifact artifact, Class<T> elementType) {
        if (elementType == null) {
            throw new IllegalArgumentException("elementType cannot be null");
        }
        Object value = get(artifact).orElse(null);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> values)) {
            throw new IllegalStateException("Pipeline artifact is not a list: " + artifact);
        }
        return values.stream().map(elementType::cast).toList();
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
        if (value instanceof ua.demo.agentlab.ui.discovery.spa.agent.SpaInventoryOutput output) {
            putIfPresent(WorkflowArtifact.SPA_PAGE_INVENTORY, output.inventory());
            putIfPresent(WorkflowArtifact.SPA_INVENTORY_PERSISTENCE, output.persistence());
            return;
        }
        if (value instanceof ua.demo.agentlab.ui.discovery.spa.agent.SpaTargetedVerificationOutput output) {
            putIfPresent(WorkflowArtifact.SPA_TARGETED_VERIFICATION, output.verification());
            putIfPresent(WorkflowArtifact.SPA_EVIDENCE_LIFECYCLE, output.lifecycle());
            return;
        }
        if (value instanceof ua.demo.agentlab.ui.discovery.spa.agent.SpaSourceStateBindingOutput output) {
            putIfPresent(WorkflowArtifact.SPA_SOURCE_STATE_BINDINGS, output.bindings());
            return;
        }
        if (value instanceof ua.demo.agentlab.ui.discovery.spa.agent.SpaLiveTargetedVerificationOutput output) {
            putIfPresent(WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION, output.result());
            putIfPresent(WorkflowArtifact.SPA_LIVE_TRANSITION_DISCOVERY, output.transitionDiscovery());
            putIfPresent(WorkflowArtifact.SPA_STRUCTURED_BEHAVIOR_EXECUTION, output.behaviorExecution());
            putIfPresent(WorkflowArtifact.SPA_EVIDENCE_LIFECYCLE, output.lifecycle());
            return;
        }
        if (value instanceof ua.demo.agentlab.ui.discovery.spa.agent.SpaTargetStateBindingOutput output) {
            putIfPresent(WorkflowArtifact.SPA_TARGET_STATE_BINDINGS, output.bindings());
            putIfPresent(WorkflowArtifact.SPA_STRUCTURED_BEHAVIOR_BINDINGS, output.bindings().behaviorBindings());
            putIfPresent(WorkflowArtifact.SPA_EFFECTIVE_PAGE_INVENTORY, output.effectiveInventory());
            putIfPresent(WorkflowArtifact.SPA_REBOUND_SOURCE_STATE_BINDINGS, output.reboundSources());
            return;
        }
        if (value instanceof ua.demo.agentlab.ui.discovery.spa.agent.SpaComponentInteractionGraphOutput output) {
            putIfPresent(WorkflowArtifact.SPA_COMPONENT_INTERACTION_GRAPH, output.graph());
            return;
        }
        if (value instanceof ua.demo.agentlab.ui.discovery.interaction.agent.UiInteractionEvidenceOutput output) {
            putIfPresent(WorkflowArtifact.CANONICAL_INTERACTION_EVIDENCE, output.canonical());
            putIfPresent(WorkflowArtifact.LOCATOR_CANDIDATE_COVERAGE_REPORT, output.coverage());
            putIfPresent(WorkflowArtifact.CONFIRMED_UI_CATALOG, output.catalog());
            putIfPresent(WorkflowArtifact.INTERACTION_GRAPH_PROJECTION, output.graphProjection());
            putIfPresent(WorkflowArtifact.EVIDENCE_PROJECTION_TRACE, output.projectionTrace());
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
        if (value instanceof ua.demo.agentlab.testcase.agent.RequirementToTestCaseOutput output) {
            putIfPresent(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE, output.canonicalTestCaseBundle());
            putIfPresent(WorkflowArtifact.REQUIREMENT_GOVERNANCE_BUNDLE, output.governanceBundle());
            return;
        }
        if (value instanceof ua.demo.agentlab.ai.context.AiContextPackage contextPackage) {
            putIfPresent(WorkflowArtifact.AI_CONTEXT_PACKAGE, contextPackage);
            return;
        }
        if (value instanceof ua.demo.agentlab.ai.ui.generation.AiPageObjectGenerationResult result) {
            putIfPresent(WorkflowArtifact.AI_PAGE_OBJECT_SPECS, result.specs());
            putIfPresent(WorkflowArtifact.POM_CONTRACT_SPECS, result.contracts());
            return;
        }
        if (value instanceof ua.demo.agentlab.ai.ui.generation.AiUiTestGenerationResult result) {
            putIfPresent(WorkflowArtifact.AI_UI_TEST_SPECS, result.specs());
            return;
        }
        if (value instanceof ua.demo.agentlab.ui.testcontract.validation.UiTestContractValidationResult result) {
            putIfPresent(WorkflowArtifact.UI_TEST_CONTRACT_SCHEMA_VALIDATION, result.schemaReport());
            putIfPresent(WorkflowArtifact.UI_TEST_CONTRACT_QUALITY_REPORT, result.qualityReport());
            return;
        }
        if (value instanceof ua.demo.agentlab.ui.testcontract.writer.DeterministicTestNgGenerationResult result) {
            putIfPresent(WorkflowArtifact.GENERATED_UI_TEST_SOURCES, result.files());
            putIfPresent(WorkflowArtifact.UI_TEST_FILES, result.files());
            putIfPresent(WorkflowArtifact.UI_TEST_SOURCE_MAP, result.sourceMap());
            return;
        }
        if (value instanceof java.util.List list) {
            if (artifact == WorkflowArtifact.PAGE_OBJECT_FILES) {
                putIfPresent(WorkflowArtifact.PAGE_OBJECT_FILES, list);
                putIfPresent(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES, list);
            } else if (artifact == WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES) {
                putIfPresent(WorkflowArtifact.PAGE_OBJECT_FILES, list);
                putIfPresent(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES, list);
            } else if (artifact == WorkflowArtifact.UI_TEST_FILES) {
                putIfPresent(WorkflowArtifact.UI_TEST_FILES, list);
            } else if (artifact == WorkflowArtifact.WRITTEN_FILES) {
                putIfPresent(WorkflowArtifact.WRITTEN_FILES, list);
                putIfPresent(WorkflowArtifact.PERSISTED_GENERATED_SOURCES, list);
            } else if (artifact == WorkflowArtifact.PERSISTED_GENERATED_SOURCES) {
                putIfPresent(WorkflowArtifact.WRITTEN_FILES, list);
                putIfPresent(WorkflowArtifact.PERSISTED_GENERATED_SOURCES, list);
            }
            return;
        }
        if (value instanceof ua.demo.agentlab.validation.GeneratedCodeValidationResult result) {
            putIfPresent(WorkflowArtifact.GENERATED_CODE_VALIDATION, result);
            putIfPresent(WorkflowArtifact.COMPILE_RESULT, result);
            return;
        }
        if (value instanceof ua.demo.agentlab.review.GeneratedCodeReviewReport report) {
            putIfPresent(WorkflowArtifact.GENERATED_CODE_REVIEW, report);
            putIfPresent(WorkflowArtifact.REVIEW_RESULT, report);
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
        putIfPresent(WorkflowArtifact.REQUIREMENT_GOVERNANCE_BUNDLE, state.getRequirementGovernanceBundle());
        putIfPresent(WorkflowArtifact.UI_KNOWLEDGE_PERSISTED, state.getArtifacts().get("ui.knowledge.persistence.completed"));
        putIfPresent(WorkflowArtifact.ASSERTION_CONTRACTS, state.getAssertionContracts());
        putIfPresent(WorkflowArtifact.UI_TEST_PLAN, state.getUiTestPlan());
        putIfPresent(WorkflowArtifact.AI_CONTEXT_PACKAGE, state.getAiContextPackage());
        putIfPresent(WorkflowArtifact.POM_CONTRACT_SPECS, state.getPomContractSpecs());
        putIfPresent(WorkflowArtifact.AI_PAGE_OBJECT_SPECS, state.getAiPageObjectSpecs());
        putIfPresent(WorkflowArtifact.AI_UI_TEST_SPECS, state.getAiUiTestSpecs());
        putIfPresent(WorkflowArtifact.PAGE_OBJECT_FILES, state.getPageObjectFiles());
        putIfPresent(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES, state.getPageObjectFiles());
        putIfPresent(WorkflowArtifact.UI_TEST_FILES, state.getUiTestFiles());
        putIfPresent(WorkflowArtifact.GENERATED_UI_CONTRACT_VALIDATION, state.getGeneratedUiContractValidationResult());
        putIfPresent(WorkflowArtifact.GENERATED_CODE_VALIDATION, state.getGeneratedCodeValidationResult());
        putIfPresent(WorkflowArtifact.COMPILE_RESULT, state.getGeneratedCodeValidationResult());
        putIfPresent(WorkflowArtifact.GENERATED_CODE_REVIEW, state.getGeneratedCodeReviewReport());
        putIfPresent(WorkflowArtifact.REVIEW_RESULT, state.getGeneratedCodeReviewReport());
        putIfPresent(WorkflowArtifact.WRITTEN_FILES, state.getWrittenFiles());
        putIfPresent(WorkflowArtifact.PERSISTED_GENERATED_SOURCES, state.getWrittenFiles());
    }

    private void putIfPresent(WorkflowArtifact artifact, Object value) {
        if (value != null) {
            artifacts.put(artifact, value);
        }
    }

}
