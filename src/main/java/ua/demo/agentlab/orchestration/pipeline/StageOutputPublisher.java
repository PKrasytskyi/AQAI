package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.debug.AiRunArtifactWriter;
import ua.demo.agentlab.ai.expectationenrichment.agent.TestCaseExpectationEnrichmentOutput;
import ua.demo.agentlab.ai.expectationenrichment.model.ResolvedExpectedResult;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.ai.pageenrichment.agent.PageModelEnrichmentOutput;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheLookupResult;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectGenerationResult;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.discovery.agent.UiDiscoveryOutput;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.PageKnowledgeWriteResult;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelArtifactWriter;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StageOutputPublisher {

    private final AiRunArtifactWriter aiArtifactWriter = new AiRunArtifactWriter();

    public void publishRequirementDocument(RequirementDocument document, WorkflowState state) {
        if (state == null || document == null) {
            return;
        }
        state.setRequirementDocument(document);
        state.addArtifact("requirements.raw", document.content());
        state.addFinding("Requirements loaded from: " + document.source());
    }

    public void publishNormalizedRequirementBundle(NormalizedRequirementBundle bundle, WorkflowState state) {
        if (state == null || bundle == null) {
            return;
        }
        state.setNormalizedRequirementBundle(bundle);
        state.addArtifact("requirements.normalized.source", bundle.source());
        state.addArtifact("requirements.normalized.count", String.valueOf(bundle.requirements().size()));
        state.addArtifact("requirements.normalized.assumptions", String.valueOf(bundle.assumptions().size()));
        state.addArtifact("requirements.normalized.risks", String.valueOf(bundle.risks().size()));
        state.addFinding("Requirements normalized: " + bundle.requirements().size());
    }

    public void publishGenerationPolicy(GenerationPolicy policy, WorkflowState state) {
        if (state == null || policy == null) {
            return;
        }
        state.setGenerationPolicy(policy);
        state.addArtifact("policy.id", policy.policyId());
        state.addArtifact("policy.description", policy.description());
        state.addArtifact("policy.ui.framework", policy.frameworkPolicy().uiFramework().name());
        state.addArtifact("policy.api.framework", policy.frameworkPolicy().apiFramework().name());
        state.addArtifact("policy.test.style", policy.frameworkPolicy().testStyle().name());
        state.addArtifact("policy.selector.order", policy.selectorPolicy().priorityOrder()
                .stream()
                .map(Enum::name)
                .collect(Collectors.joining(" -> "))
        );
        state.addFinding("Generation policy loaded: " + policy.policyId());
    }

    public void publishUiDiscoveryOutput(UiDiscoveryOutput output, WorkflowState state) {
        if (state == null || output == null || output.discoverySnapshot() == null) {
            return;
        }
        state.setUiDiscoverySnapshot(output.discoverySnapshot());
        state.setSeleniumDiscoveryResult(output.seleniumDiscoveryResult());
        state.setCanonicalPageFlowModel(output.canonicalPageFlowModel());
        state.addArtifact("ui.discovery.page.count", String.valueOf(output.discoverySnapshot().pages().size()));
        state.addArtifact("ui.discovery.flow.count", String.valueOf(output.discoverySnapshot().flows().size()));
        if (output.canonicalPageFlowModel() != null) {
            state.addArtifact("ui.canonical.page.count", String.valueOf(output.canonicalPageFlowModel().pages().size()));
            state.addArtifact("ui.canonical.flow.count", String.valueOf(output.canonicalPageFlowModel().flows().size()));
            state.addFinding("Canonical UI model prepared with " + output.canonicalPageFlowModel().flows().size() + " flows");
        }
        state.addFinding("UI discovery identified " + output.discoverySnapshot().pages().size() + " candidate pages");
        if (output.seleniumDiscoveryResult() != null) {
            state.addArtifact("ui.discovery.transition.count",
                    String.valueOf(output.seleniumDiscoveryResult().transitions().size()));
            state.addArtifact("ui.discovery.repeat.run.count",
                    String.valueOf(output.seleniumDiscoveryResult().discoveryRunCount()));
            state.addArtifact("ui.discovery.locator.observation.count",
                    String.valueOf(output.seleniumDiscoveryResult().locatorObservationCounts().size()));
            state.addFinding("Selenium discovery captured "
                    + output.seleniumDiscoveryResult().pages().size() + " raw page snapshots");
            state.addFinding("Selenium discovery stability gate aggregated "
                    + output.seleniumDiscoveryResult().discoveryRunCount() + " run(s)");
        }
    }

    public void publishCanonicalTestCaseBundle(CanonicalTestCaseBundle bundle, WorkflowState state) {
        if (state == null || bundle == null) {
            return;
        }
        state.setCanonicalTestCaseBundle(bundle);
        state.addArtifact("canonical.test.case.primary.page", bundle.primaryPage());
        state.addArtifact("canonical.test.case.pages", String.join(", ", bundle.pageNames()));
        state.addArtifact("canonical.test.case.count", String.valueOf(bundle.testCases().size()));
        state.addFinding("Canonical test case bundle created with " + bundle.testCases().size() + " test case(s)");
    }

    public void publishExpectationEnrichment(TestCaseExpectationEnrichmentOutput output, WorkflowState state) {
        if (state == null || output == null || output.bundle() == null) {
            return;
        }
        state.setCanonicalTestCaseBundle(output.bundle());
        long approved = output.resolved().stream().filter(result -> result.isApproved()).count();
        state.addArtifact("test.case.expectation.candidate.count", String.valueOf(output.candidates().size()));
        state.addArtifact("test.case.expectation.resolved.count", String.valueOf(approved));
        state.addArtifact("test.case.expectation.failures", String.valueOf(output.failures().size()));
        state.addArtifact("llm.schema.resolved.expected.result.version", LlmOutputSchemaVersion.RESOLVED_EXPECTED_RESULT);
        state.addAiArtifactFile(aiArtifactWriter.writeJson(
                "expectations",
                "test-case-expected-results.json",
                output.resolved()
        ).toString());
        state.addAiArtifactFile(aiArtifactWriter.writeJson(
                "expectations",
                "expected-result-candidates.json",
                output.candidates()
        ).toString());
        List<ResolvedExpectedResult> needsReview = output.resolved().stream()
                .filter(result -> !result.isApproved())
                .toList();
        state.addArtifact("test.case.expectation.needs.review.count", String.valueOf(needsReview.size()));
        if (!needsReview.isEmpty()) {
            state.addAiArtifactFile(aiArtifactWriter.writeJson(
                    "need-review",
                    "expected-results-needs-review.json",
                    needsReview
            ).toString());
            for (ResolvedExpectedResult result : needsReview) {
                state.addAiArtifactFile(aiArtifactWriter.writeJson(
                        "need-review",
                        safeFileName(result.testCaseId()) + "-expected-result.json",
                        result
                ).toString());
            }
        }
        state.addFinding("Expected-result enrichment resolved "
                + approved + " of " + output.resolved().size() + " canonical test case(s)");
    }

    private String safeFileName(String value) {
        String normalized = value == null || value.isBlank() ? "unknown" : value.trim();
        normalized = normalized.replaceAll("[^A-Za-z0-9._-]+", "-");
        normalized = normalized.replaceAll("(^[.-]+|[.-]+$)", "");
        return normalized.isBlank() ? "unknown" : normalized;
    }

    public void publishPageModelBundle(
            PageModelBundle pageModelBundle,
            WorkflowState state,
            PageModelArtifactWriter artifactWriter
    ) {
        if (state == null || pageModelBundle == null) {
            return;
        }
        state.setPageModelBundle(pageModelBundle);
        List<String> writtenFiles = artifactWriter == null ? List.of() : artifactWriter.write(pageModelBundle);
        int elementCount = pageModelBundle.pages().stream()
                .mapToInt(page -> page.elements().size())
                .sum();
        int formCount = pageModelBundle.pages().stream()
                .mapToInt(page -> page.forms().size())
                .sum();
        int flowCount = pageModelBundle.pages().stream()
                .mapToInt(page -> page.flows().size())
                .sum();

        state.addArtifact("ui.page.model.page.count", String.valueOf(pageModelBundle.pages().size()));
        state.addArtifact("ui.page.model.element.count", String.valueOf(elementCount));
        state.addArtifact("ui.page.model.form.count", String.valueOf(formCount));
        state.addArtifact("ui.page.model.flow.count", String.valueOf(flowCount));
        state.addArtifact("ui.page.model.artifact.count", String.valueOf(writtenFiles.size()));
        state.addArtifact("ui.page.model.artifact.files", String.join(",", writtenFiles));
        state.addFinding("PageModel prepared " + pageModelBundle.pages().size()
                + " page(s), " + elementCount + " element(s), " + formCount + " form(s)");
    }

    public void publishMappedUiKnowledge(MappedUiKnowledge mappedUiKnowledge, WorkflowState state) {
        if (state == null || mappedUiKnowledge == null) {
            return;
        }
        state.setMappedUiKnowledge(mappedUiKnowledge);
        state.addArtifact("ui.mapped.page.count", String.valueOf(mappedUiKnowledge.pages().size()));
        state.addArtifact("ui.mapped.transition.count", String.valueOf(mappedUiKnowledge.transitions().size()));
        state.addArtifact("ui.mapped.graph.node.count", String.valueOf(mappedUiKnowledge.graphNodes().size()));
        state.addArtifact("ui.mapped.graph.edge.count", String.valueOf(mappedUiKnowledge.graphEdges().size()));
        state.addArtifact("ui.mapped.vector.document.count", String.valueOf(mappedUiKnowledge.vectorDocuments().size()));
        state.addFinding("Page mapper prepared " + mappedUiKnowledge.pages().size() + " mapped UI page(s)");
    }

    public void publishFlowScopedKnowledgePackage(FlowScopedKnowledgePackage knowledgePackage, WorkflowState state) {
        if (state == null || knowledgePackage == null) {
            return;
        }
        state.setFlowScopedKnowledgePackage(knowledgePackage);
        state.addArtifact("flow.scope.page.count", String.valueOf(knowledgePackage.mappedUiKnowledge().pages().size()));
        state.addArtifact("flow.scope.interaction.count",
                String.valueOf(knowledgePackage.canonicalInteractionModel().interactions().size()));
        state.addArtifact("flow.scope.vector.match.count",
                String.valueOf(knowledgePackage.retrievalContext().vectorMatches().size()));
        state.addArtifact("flow.scope.graph.match.count",
                String.valueOf(knowledgePackage.retrievalContext().graphMatches().size()));
        state.addFinding("Flow-scoped knowledge package prepared for AI context");
        state.addAiArtifactFile(aiArtifactWriter.writeJson(
                "flow-scoped-knowledge",
                "flow-scoped-knowledge-package.json",
                knowledgePackage
        ).toString());
    }

    public void publishRefreshedFlowScopedKnowledgePackage(FlowScopedKnowledgePackage knowledgePackage, WorkflowState state) {
        if (state == null || knowledgePackage == null) {
            return;
        }
        state.setFlowScopedKnowledgePackage(knowledgePackage);
        state.addArtifact("flow.scoped.knowledge.refreshed", "true");
        state.addFinding("Flow-scoped knowledge refreshed after UI knowledge persistence");
    }

    public void publishPageKnowledgeCacheLookup(
            PageKnowledgeCacheLookupResult result,
            WorkflowState state,
            KnowledgeRunMetadata runMetadata,
            String retrievalMode
    ) {
        if (state == null || result == null) {
            return;
        }
        if (runMetadata != null) {
            state.setKnowledgeRunMetadata(runMetadata);
        }
        state.setPageKnowledgeCacheLookupResult(result);
        state.addArtifact("page.knowledge.cache.retrieval.mode", retrievalMode == null ? "" : retrievalMode);
        state.addArtifact("page.knowledge.cache.hit.count", String.valueOf(result.hitCount()));
        state.addArtifact("page.knowledge.cache.miss.count", String.valueOf(result.missCount()));
        state.addAiArtifactFile(aiArtifactWriter.writeJson(
                "enrichment",
                "page-knowledge-cache-lookup.json",
                result
        ).toString());
        state.addFinding("Page knowledge cache lookup completed: hits="
                + result.hitCount() + ", misses=" + result.missCount());
    }

    public void publishPageModelEnrichment(PageModelEnrichmentOutput output, WorkflowState state) {
        if (state == null || output == null) {
            return;
        }
        state.setPageModelEnrichments(output.records());
        state.setEnrichedMappedUiKnowledge(output.enrichedMappedUiKnowledge());
        state.addArtifact("page.enrichment.page.count", String.valueOf(output.records().size()));
        state.addArtifact("page.enrichment.cache.hit.count", String.valueOf(output.cachedRecords().size()));
        state.addArtifact("page.enrichment.generated.count", String.valueOf(output.generatedRecords().size()));
        state.addArtifact("llm.schema.page.model.enrichment.version", LlmOutputSchemaVersion.PAGE_MODEL_ENRICHMENT_RECORD);
        state.addArtifact("page.enrichment.openai.count", String.valueOf(output.records().stream()
                .filter(record -> "openai".equals(record.enrichmentSource())).count()));
        state.addArtifact("page.enrichment.failures", String.valueOf(output.failures().size()));
        state.addAiArtifactFile(aiArtifactWriter.writeJson("enrichment", "page-model-enrichments.json", output.records()).toString());
        state.addAiArtifactFile(aiArtifactWriter.writeJson(
                "enrichment",
                "page-model-enrichment-report.json",
                Map.of("records", output.records().size(), "openAiRecords", output.records().stream()
                        .filter(record -> "openai".equals(record.enrichmentSource())).count(), "failures", output.failures())
        ).toString());
        state.addFinding("PageModel enrichment prepared "
                + output.records().size() + " requirement-scoped page record(s)");
    }

    public void publishKnowledgePersistence(
            List<PageKnowledgeWriteResult> results,
            WorkflowState state,
            KnowledgeRunMetadata runMetadata
    ) {
        if (state == null) {
            return;
        }
        if (runMetadata != null) {
            state.setKnowledgeRunMetadata(runMetadata);
            state.addArtifact("ui.knowledge.run.id", runMetadata.runId());
            state.addArtifact("ui.knowledge.app.id", runMetadata.appId());
            state.addArtifact("ui.knowledge.schema.version", runMetadata.schemaVersion());
            state.addArtifact("ui.knowledge.requirement.set.hash", runMetadata.requirementSetHash());
            state.addArtifact("ui.knowledge.base.url.hash", runMetadata.baseUrlHash());
            state.addArtifact("ui.knowledge.discovery.session.id", runMetadata.discoverySessionId());
        }
        int executedTargets = 0;
        for (PageKnowledgeWriteResult result : results == null ? List.<PageKnowledgeWriteResult>of() : results) {
            state.addArtifact("ui.knowledge." + result.target() + ".executed", String.valueOf(result.executed()));
            state.addArtifact("ui.knowledge." + result.target() + ".node.count", String.valueOf(result.nodeCount()));
            state.addArtifact("ui.knowledge." + result.target() + ".edge.count", String.valueOf(result.edgeCount()));
            state.addArtifact("ui.knowledge." + result.target() + ".document.count", String.valueOf(result.documentCount()));
            if (!result.details().isBlank()) {
                state.addFinding(result.target().toUpperCase() + ": " + result.details());
            }
            if (result.executed()) {
                executedTargets++;
            }
        }
        state.addArtifact("ui.knowledge.persistence.completed", "true");
        state.addArtifact("ui.knowledge.persistence.executed.target.count", String.valueOf(executedTargets));
    }

    public void publishAssertionContracts(List<AssertionContract> contracts, WorkflowState state) {
        if (state == null || contracts == null) {
            return;
        }
        state.setAssertionContracts(contracts);
        state.addArtifact("assertion.contract.count", String.valueOf(contracts.size()));
        state.addAiArtifactFile(aiArtifactWriter.writeJson("expectations", "assertion-contracts.json", contracts).toString());
        state.addFinding("Assertion contracts prepared " + contracts.size() + " typed assertion contract(s)");
    }

    public void publishAiContextPackage(AiContextPackage contextPackage, WorkflowState state) {
        if (state == null || contextPackage == null) {
            return;
        }
        state.setAiContextPackage(contextPackage);
        state.addArtifact("ai.context.ready", "true");
        state.addArtifact(
                "ai.context.canonical.interactions",
                String.valueOf(contextPackage.canonicalInteractionModel().interactions().size())
        );
        state.addArtifact(
                "ai.context.retrieval.vector.matches",
                String.valueOf(contextPackage.retrievalContext().vectorMatches().size())
        );
        state.addArtifact(
                "ai.context.retrieval.graph.matches",
                String.valueOf(contextPackage.retrievalContext().graphMatches().size())
        );
        state.addFinding("AI context package assembled from typed pipeline input");
        state.addAiArtifactFile(aiArtifactWriter.writeJson(
                "context",
                "ai-context-package.json",
                contextPackage
        ).toString());
    }

    public void publishAiPageObjectSpecs(List<AiPageObjectSpec> specs, WorkflowState state) {
        if (state == null || specs == null) {
            return;
        }
        state.setAiPageObjectSpecs(specs);
        state.addArtifact("ai.page.object.spec.count", String.valueOf(specs.size()));
    }

    public void publishAiPageObjectGenerationResult(AiPageObjectGenerationResult result, WorkflowState state) {
        if (state == null || result == null) {
            return;
        }
        state.setAiPageObjectSpecs(result.specs());
        result.artifactFiles().forEach(state::addAiArtifactFile);
        result.artifacts().forEach(state::addArtifact);
        result.findings().forEach(state::addFinding);
        state.addArtifact("ai.page.object.spec.count", String.valueOf(result.specs().size()));
    }
}
