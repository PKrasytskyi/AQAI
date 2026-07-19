package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.expectationenrichment.agent.TestCaseExpectationEnrichmentOutput;
import ua.demo.agentlab.ai.expectationenrichment.model.ResolvedExpectedResult;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.ai.pageenrichment.agent.PageModelEnrichmentOutput;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheLookupResult;
import ua.demo.agentlab.ai.rag.intelligence.agent.RepositoryIntelligenceEnrichmentResult;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectGenerationResult;
import ua.demo.agentlab.ai.ui.generation.AiUiTestGenerationResult;
import ua.demo.agentlab.api.agent.ApiGenerationResult;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.testcase.agent.RequirementToTestCaseOutput;
import ua.demo.agentlab.testcase.governance.RequirementGovernanceBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.discovery.agent.UiPageMappingOutput;
import ua.demo.agentlab.ui.discovery.agent.UiDiscoveryOutput;
import ua.demo.agentlab.ui.discovery.agent.UiDiscoveryArtifactPersistenceResult;
import ua.demo.agentlab.ui.discovery.component.ComponentBoundaryDetector;
import ua.demo.agentlab.ui.discovery.component.ComponentModelArtifactWriter;
import ua.demo.agentlab.ui.discovery.component.model.ComponentDiscoveryModel;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.PageKnowledgeWriteResult;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelArtifactWriter;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.runtime.RuntimeEvidenceArtifactWriter;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;
import ua.demo.agentlab.ui.discovery.runtime.feedback.RuntimeFeedbackAnalyzer;
import ua.demo.agentlab.ui.discovery.runtime.feedback.RuntimeFeedbackArtifactWriter;
import ua.demo.agentlab.ui.discovery.runtime.feedback.RuntimeFeedbackSummary;
import ua.demo.agentlab.ui.discovery.spa.agent.SpaInventoryOutput;
import ua.demo.agentlab.ui.discovery.spa.agent.SpaTargetedVerificationOutput;
import ua.demo.agentlab.ui.discovery.semantic.SemanticActionModelArtifactWriter;
import ua.demo.agentlab.ui.discovery.semantic.SemanticActionModelBuilder;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticActionModel;
import ua.demo.agentlab.ui.discovery.semanticgraph.SemanticGraphArtifactWriter;
import ua.demo.agentlab.ui.discovery.semanticgraph.SemanticGraphBuilder;
import ua.demo.agentlab.ui.discovery.semanticgraph.model.SemanticGraphModel;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.GeneratedUiContractValidationResult;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class StageOutputPublisher {

    private final AiArtifactPublisher aiArtifactPublisher = new AiArtifactPublisher();
    private final SemanticActionModelBuilder semanticActionModelBuilder = new SemanticActionModelBuilder();
    private final SemanticActionModelArtifactWriter semanticActionModelArtifactWriter =
            new SemanticActionModelArtifactWriter();
    private final SemanticGraphBuilder semanticGraphBuilder = new SemanticGraphBuilder();
    private final SemanticGraphArtifactWriter semanticGraphArtifactWriter = new SemanticGraphArtifactWriter();
    private final RuntimeFeedbackAnalyzer runtimeFeedbackAnalyzer = new RuntimeFeedbackAnalyzer();
    private final RuntimeFeedbackArtifactWriter runtimeFeedbackArtifactWriter = new RuntimeFeedbackArtifactWriter();
    private final ComponentBoundaryDetector componentBoundaryDetector = new ComponentBoundaryDetector();
    private final ComponentModelArtifactWriter componentModelArtifactWriter = new ComponentModelArtifactWriter();

    public void publishRequirementDocument(RequirementDocument document, WorkflowState state) {
        if (state == null || document == null) {
            return;
        }
        state.setRequirementDocument(document);
        putArtifact(state, "requirements.raw", document.content());
        addFinding(state, "Requirements loaded from: " + document.source());
    }

    public void publishNormalizedRequirementBundle(NormalizedRequirementBundle bundle, WorkflowState state) {
        if (state == null || bundle == null) {
            return;
        }
        state.setNormalizedRequirementBundle(bundle);
        putArtifact(state, "requirements.normalized.source", bundle.source());
        putArtifact(state, "requirements.normalized.count", String.valueOf(bundle.requirements().size()));
        putArtifact(state, "requirements.normalized.assumptions", String.valueOf(bundle.assumptions().size()));
        putArtifact(state, "requirements.normalized.risks", String.valueOf(bundle.risks().size()));
        addFinding(state, "Requirements normalized: " + bundle.requirements().size());
    }

    public void publishGenerationPolicy(GenerationPolicy policy, WorkflowState state) {
        if (state == null || policy == null) {
            return;
        }
        state.setGenerationPolicy(policy);
        putArtifact(state, "policy.id", policy.policyId());
        putArtifact(state, "policy.description", policy.description());
        putArtifact(state, "policy.ui.framework", policy.frameworkPolicy().uiFramework().name());
        putArtifact(state, "policy.api.framework", policy.frameworkPolicy().apiFramework().name());
        putArtifact(state, "policy.test.style", policy.frameworkPolicy().testStyle().name());
        putArtifact(state, "policy.selector.order", policy.selectorPolicy().priorityOrder()
                .stream()
                .map(Enum::name)
                .collect(Collectors.joining(" -> "))
        );
        addFinding(state, "Generation policy loaded: " + policy.policyId());
    }

    public void publishTestPlan(TestPlan testPlan, WorkflowState state) {
        if (state == null || testPlan == null) {
            return;
        }
        state.setTestPlan(testPlan);
        putArtifact(state, "test.plan.source", testPlan.source());
        putArtifact(
                state,
                "test.plan.summary",
                "areas=%d, scenarios=%d, assumptions=%d, risks=%d".formatted(
                        testPlan.functionalAreas().size(),
                        testPlan.scenarios().size(),
                        testPlan.assumptions().size(),
                        testPlan.risks().size()
                )
        );
        addFinding(state, "Test plan created with " + testPlan.scenarios().size() + " scenarios");
    }

    public void publishUiDiscoveryOutput(UiDiscoveryOutput output, WorkflowState state) {
        if (state == null || output == null || output.discoverySnapshot() == null) {
            return;
        }
        state.setUiDiscoverySnapshot(output.discoverySnapshot());
        state.setSeleniumDiscoveryResult(output.seleniumDiscoveryResult());
        state.setCanonicalPageFlowModel(output.canonicalPageFlowModel());
        putArtifact(state, "ui.discovery.page.count", String.valueOf(output.discoverySnapshot().pages().size()));
        putArtifact(state, "ui.discovery.flow.count", String.valueOf(output.discoverySnapshot().flows().size()));
        if (output.canonicalPageFlowModel() != null) {
            putArtifact(state, "ui.canonical.page.count", String.valueOf(output.canonicalPageFlowModel().pages().size()));
            putArtifact(state, "ui.canonical.flow.count", String.valueOf(output.canonicalPageFlowModel().flows().size()));
            addFinding(state, "Canonical UI model prepared with " + output.canonicalPageFlowModel().flows().size() + " flows");
        }
        addFinding(state, "UI discovery identified " + output.discoverySnapshot().pages().size() + " candidate pages");
        if (output.seleniumDiscoveryResult() != null) {
            putArtifact(state, "ui.discovery.transition.count",
                    String.valueOf(output.seleniumDiscoveryResult().transitions().size()));
            putArtifact(state, "ui.discovery.repeat.run.count",
                    String.valueOf(output.seleniumDiscoveryResult().discoveryRunCount()));
            putArtifact(state, "ui.discovery.locator.observation.count",
                    String.valueOf(output.seleniumDiscoveryResult().locatorObservationCounts().size()));
            addFinding(state, "Selenium discovery captured "
                    + output.seleniumDiscoveryResult().pages().size() + " raw page snapshots");
            addFinding(state, "Selenium discovery stability gate aggregated "
                    + output.seleniumDiscoveryResult().discoveryRunCount() + " run(s)");
        }
    }

    public void publishRuntimeEvidence(
            RuntimeEvidenceBundle bundle,
            WorkflowState state,
            RuntimeEvidenceArtifactWriter artifactWriter
    ) {
        if (state == null || bundle == null) {
            return;
        }
        state.setRuntimeEvidenceBundle(bundle);
        List<String> writtenFiles = artifactWriter == null ? List.of() : artifactWriter.write(bundle);
        putArtifact(state, "ui.runtime.network.request.count", String.valueOf(bundle.networkRequests().size()));
        putArtifact(state, "ui.runtime.network.response.count", String.valueOf(bundle.networkResponses().size()));
        putArtifact(state, "ui.runtime.console.log.count", String.valueOf(bundle.consoleLogs().size()));
        putArtifact(state, "ui.runtime.semantic.network.count", String.valueOf(bundle.semanticNetworkEvidence().size()));
        putArtifact(state, "ui.runtime.state.transition.count", String.valueOf(bundle.stateTransitions().size()));
        putArtifact(state, "ui.runtime.artifact.files", String.join(",", writtenFiles));
        addFinding(state, "Runtime evidence captured "
                + bundle.networkResponses().size() + " network response(s), "
                + bundle.semanticNetworkEvidence().size() + " semantic network fact(s), "
                + bundle.stateTransitions().size() + " SPA/state transition(s)");
    }

    public void publishCanonicalTestCaseBundle(CanonicalTestCaseBundle bundle, WorkflowState state) {
        if (state == null || bundle == null) {
            return;
        }
        state.setCanonicalTestCaseBundle(bundle);
        putArtifact(state, "canonical.test.case.primary.page", bundle.primaryPage());
        putArtifact(state, "canonical.test.case.pages", String.join(", ", bundle.pageNames()));
        putArtifact(state, "canonical.test.case.count", String.valueOf(bundle.testCases().size()));
        addFinding(state, "Canonical test case bundle created with " + bundle.testCases().size() + " test case(s)");
    }

    public void publishRequirementToTestCaseOutput(RequirementToTestCaseOutput output, WorkflowState state) {
        if (state == null || output == null) {
            return;
        }
        publishCanonicalTestCaseBundle(output.canonicalTestCaseBundle(), state);
        RequirementGovernanceBundle governance = output.governanceBundle();
        state.setRequirementGovernanceBundle(governance);
        putArtifact(state, "requirement.governance.total.count", String.valueOf(governance.totalRequirements()));
        putArtifact(
                state,
                "requirement.governance.test.case.eligible.count",
                String.valueOf(governance.canonicalTestCaseRequirements())
        );
        putArtifact(
                state,
                "requirement.governance.non.test.case.count",
                String.valueOf(governance.governanceRequirements().size())
        );
        aiArtifactPublisher.writeJson(
                state,
                "requirements",
                "requirement-governance-bundle.json",
                governance
        );
        aiArtifactPublisher.writeJson(
                state,
                "requirements",
                "canonical-test-case-requirements.json",
                governance.testCaseRequirements()
        );
        aiArtifactPublisher.writeJson(
                state,
                "requirements",
                "requirement-governance-items.json",
                governance.governanceRequirements()
        );
        addFinding(state, "Requirement governance classified "
                + governance.canonicalTestCaseRequirements()
                + " executable requirement(s) and "
                + governance.governanceRequirements().size()
                + " governance/context requirement(s)");
    }

    public void publishExpectationEnrichment(TestCaseExpectationEnrichmentOutput output, WorkflowState state) {
        if (state == null || output == null || output.bundle() == null) {
            return;
        }
        state.setCanonicalTestCaseBundle(output.bundle());
        long approved = output.resolved().stream().filter(result -> result.isApproved()).count();
        putArtifact(state, "test.case.expectation.candidate.count", String.valueOf(output.candidates().size()));
        putArtifact(state, "test.case.expectation.resolved.count", String.valueOf(approved));
        putArtifact(state, "test.case.expectation.failures", String.valueOf(output.failures().size()));
        putArtifact(state, "llm.schema.resolved.expected.result.version", LlmOutputSchemaVersion.RESOLVED_EXPECTED_RESULT);
        aiArtifactPublisher.writeJson(
                state,
                "expectations",
                "test-case-expected-results.json",
                output.resolved()
        );
        aiArtifactPublisher.writeJson(
                state,
                "expectations",
                "expected-result-candidates.json",
                output.candidates()
        );
        List<ResolvedExpectedResult> needsReview = output.resolved().stream()
                .filter(result -> !result.isApproved())
                .toList();
        putArtifact(state, "test.case.expectation.needs.review.count", String.valueOf(needsReview.size()));
        if (!needsReview.isEmpty()) {
            aiArtifactPublisher.writeJson(
                    state,
                    "need-review",
                    "expected-results-needs-review.json",
                    needsReview
            );
            for (ResolvedExpectedResult result : needsReview) {
                aiArtifactPublisher.writeJson(
                        state,
                        "need-review",
                        safeFileName(result.testCaseId()) + "-expected-result.json",
                        result
                );
            }
        }
        addFinding(state, "Expected-result enrichment resolved "
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

        putArtifact(state, "ui.page.model.page.count", String.valueOf(pageModelBundle.pages().size()));
        putArtifact(state, "ui.page.model.element.count", String.valueOf(elementCount));
        putArtifact(state, "ui.page.model.form.count", String.valueOf(formCount));
        putArtifact(state, "ui.page.model.flow.count", String.valueOf(flowCount));
        putArtifact(state, "ui.page.model.artifact.count", String.valueOf(writtenFiles.size()));
        putArtifact(state, "ui.page.model.artifact.files", String.join(",", writtenFiles));
        writeComponentModel(state, pageModelBundle);
        addFinding(state, "PageModel prepared " + pageModelBundle.pages().size()
                + " page(s), " + elementCount + " element(s), " + formCount + " form(s)");
    }

    private void writeComponentModel(WorkflowState state, PageModelBundle pageModelBundle) {
        if (state == null || pageModelBundle == null) {
            return;
        }
        ComponentDiscoveryModel componentModel = componentBoundaryDetector.detect(pageModelBundle);
        List<String> writtenFiles = componentModelArtifactWriter.write(componentModel);
        int componentCount = componentModel.pages().stream()
                .mapToInt(page -> page.components().size())
                .sum();
        int scopedLocatorCount = componentModel.pages().stream()
                .flatMap(page -> page.components().stream())
                .mapToInt(component -> component.locators().size())
                .sum();
        int componentScopedUniqueCount = componentModel.pages().stream()
                .flatMap(page -> page.components().stream())
                .flatMap(component -> component.locators().stream())
                .mapToInt(locator -> locator.uniqueWithinComponent() ? 1 : 0)
                .sum();
        putArtifact(state, "ui.component.page.count", String.valueOf(componentModel.pages().size()));
        putArtifact(state, "ui.component.count", String.valueOf(componentCount));
        putArtifact(state, "ui.component.scoped.locator.count", String.valueOf(scopedLocatorCount));
        putArtifact(state, "ui.component.scoped.unique.locator.count", String.valueOf(componentScopedUniqueCount));
        putArtifact(state, "ui.component.artifact.files", String.join(",", writtenFiles));
        addFinding(state, "Component model prepared " + componentCount
                + " component(s), " + scopedLocatorCount + " scoped locator candidate(s)");
    }

    public void publishMappedUiKnowledge(MappedUiKnowledge mappedUiKnowledge, WorkflowState state) {
        if (state == null || mappedUiKnowledge == null) {
            return;
        }
        state.setMappedUiKnowledge(mappedUiKnowledge);
        putArtifact(state, "ui.mapped.page.count", String.valueOf(mappedUiKnowledge.pages().size()));
        putArtifact(state, "ui.mapped.transition.count", String.valueOf(mappedUiKnowledge.transitions().size()));
        putArtifact(state, "ui.mapped.graph.node.count", String.valueOf(mappedUiKnowledge.graphNodes().size()));
        putArtifact(state, "ui.mapped.graph.edge.count", String.valueOf(mappedUiKnowledge.graphEdges().size()));
        putArtifact(state, "ui.mapped.vector.document.count", String.valueOf(mappedUiKnowledge.vectorDocuments().size()));
        writeSemanticActionModel(state, mappedUiKnowledge);
        addFinding(state, "Page mapper prepared " + mappedUiKnowledge.pages().size() + " mapped UI page(s)");
    }

    public void publishSpaInventory(SpaInventoryOutput output, WorkflowState state) {
        if (state == null || output == null || output.inventory() == null) {
            return;
        }
        int componentCount = output.inventory().pages().stream()
                .mapToInt(page -> page.components().size())
                .sum();
        int locatorCount = output.inventory().pages().stream()
                .flatMap(page -> page.components().stream())
                .mapToInt(component -> component.locators().size())
                .sum();
        int actionCount = output.inventory().pages().stream()
                .flatMap(page -> page.components().stream())
                .mapToInt(component -> component.actions().size())
                .sum();
        putArtifact(state, "spa.inventory.mode", output.inventory().mode().name());
        putArtifact(state, "spa.inventory.page.count", String.valueOf(output.inventory().pages().size()));
        putArtifact(state, "spa.inventory.component.count", String.valueOf(componentCount));
        putArtifact(state, "spa.inventory.candidate.locator.count", String.valueOf(locatorCount));
        putArtifact(state, "spa.inventory.candidate.action.count", String.valueOf(actionCount));
        putArtifact(state, "spa.inventory.artifact.files", String.join(",", output.writtenFiles()));
        putArtifact(state, "spa.inventory.neo4j.persisted", String.valueOf(output.persistence().executed()));
        putArtifact(state, "spa.inventory.persistence.details", output.persistence().details());
        addFinding(state, "SPA inventory prepared " + output.inventory().pages().size()
                + " page(s), " + componentCount + " component(s), " + locatorCount
                + " candidate locator(s); POM promotion remains disabled for inventory evidence");
    }

    public void publishSpaTargetedVerification(SpaTargetedVerificationOutput output, WorkflowState state) {
        if (state == null || output == null || output.verification() == null || output.lifecycle() == null) {
            return;
        }
        long verifiedLocators = output.verification().locatorVerifications().stream()
                .filter(verification -> verification.verified()).count();
        long verifiedActions = output.verification().actionVerifications().stream()
                .filter(verification -> verification.verified()).count();
        putArtifact(state, "spa.targeted.verification.locator.count",
                String.valueOf(output.verification().locatorVerifications().size()));
        putArtifact(state, "spa.targeted.verification.locator.verified.count", String.valueOf(verifiedLocators));
        putArtifact(state, "spa.targeted.verification.action.count",
                String.valueOf(output.verification().actionVerifications().size()));
        putArtifact(state, "spa.targeted.verification.action.verified.count", String.valueOf(verifiedActions));
        putArtifact(state, "spa.evidence.lifecycle.executed", String.valueOf(output.lifecycle().executed()));
        putArtifact(state, "spa.evidence.lifecycle.details", output.lifecycle().details());
        putArtifact(state, "spa.targeted.verification.artifact.files", String.join(",", output.writtenFiles()));
        addFinding(state, "SPA targeted verification processed " + verifiedLocators + "/"
                + output.verification().locatorVerifications().size() + " locator(s) and " + verifiedActions + "/"
                + output.verification().actionVerifications().size() + " action(s)");
    }

    private void writeSemanticActionModel(WorkflowState state, MappedUiKnowledge mappedUiKnowledge) {
        if (state == null || state.getPageModelBundle() == null || mappedUiKnowledge == null) {
            return;
        }
        writeComponentModel(state, state.getPageModelBundle());
        SemanticActionModel semanticActionModel = semanticActionModelBuilder.build(
                state.getPageModelBundle(),
                mappedUiKnowledge
        );
        List<String> writtenFiles = semanticActionModelArtifactWriter.write(semanticActionModel);
        SemanticGraphModel semanticGraphModel = semanticGraphBuilder.build(
                state.getPageModelBundle(),
                mappedUiKnowledge,
                semanticActionModel,
                state.getRuntimeEvidenceBundle()
        );
        List<String> semanticGraphFiles = semanticGraphArtifactWriter.write(semanticGraphModel);
        RuntimeFeedbackSummary runtimeFeedbackSummary = runtimeFeedbackAnalyzer.analyze(
                state.getPageModelBundle(),
                state.getRuntimeEvidenceBundle()
        );
        List<String> runtimeFeedbackFiles = runtimeFeedbackArtifactWriter.write(runtimeFeedbackSummary);
        int actionCount = semanticActionModel.pages().stream()
                .mapToInt(page -> page.pageActionCandidates().size())
                .sum();
        int intentCount = semanticActionModel.pages().stream()
                .mapToInt(page -> page.pageBusinessIntentCandidates().size()
                        + page.elements().stream()
                        .mapToInt(element -> element.businessIntentCandidates().size())
                        .sum())
                .sum();
        putArtifact(state, "ui.semantic.page.count", String.valueOf(semanticActionModel.pages().size()));
        putArtifact(state, "ui.semantic.action.count", String.valueOf(actionCount));
        putArtifact(state, "ui.semantic.intent.count", String.valueOf(intentCount));
        putArtifact(state, "ui.semantic.artifact.files", String.join(",", writtenFiles));
        putArtifact(state, "ui.semantic.graph.node.count", String.valueOf(semanticGraphModel.nodes().size()));
        putArtifact(state, "ui.semantic.graph.edge.count", String.valueOf(semanticGraphModel.edges().size()));
        putArtifact(state, "ui.semantic.graph.artifact.files", String.join(",", semanticGraphFiles));
        putArtifact(state, "ui.runtime.feedback.locator.pass.rate",
                String.format(java.util.Locale.ROOT, "%.2f", runtimeFeedbackSummary.locatorPassRate()));
        putArtifact(state, "ui.runtime.feedback.flaky.risk.score",
                String.format(java.util.Locale.ROOT, "%.2f", runtimeFeedbackSummary.flakyRiskScore()));
        putArtifact(state, "ui.runtime.feedback.issue.count", String.valueOf(runtimeFeedbackSummary.issues().size()));
        putArtifact(state, "ui.runtime.feedback.artifact.files", String.join(",", runtimeFeedbackFiles));
        if (!runtimeFeedbackSummary.issues().isEmpty()) {
            aiArtifactPublisher.writeJson(
                    state,
                    "need-review",
                    "runtime-feedback-needs-review.json",
                    runtimeFeedbackSummary.issues()
            );
            aiArtifactPublisher.writeJson(
                    state,
                    "need-review",
                    "runtime-feedback-review-template.json",
                    Map.of(
                            "schemaVersion", "runtime-feedback-review-v1",
                            "instructions", "Set decision to approved/rejected/needs-follow-up and add reviewedBy/rationale for each issue before promotion.",
                            "issues", runtimeFeedbackSummary.issues().stream()
                                    .map(issue -> Map.of(
                                            "severity", issue.severity(),
                                            "issueType", issue.issueType(),
                                            "pageId", issue.pageId(),
                                            "evidence", issue.evidence(),
                                            "recommendation", issue.recommendation(),
                                            "decision", "needs-review",
                                            "reviewedBy", "",
                                            "rationale", ""
                                    ))
                                    .toList()
                    )
            );
        }
    }

    public void publishMappedUiKnowledge(UiPageMappingOutput output, WorkflowState state) {
        if (state == null || output == null) {
            return;
        }
        state.setMappedUiKnowledgeRaw(output.rawKnowledge());
        state.setMappedUiKnowledgeCurated(output.curatedKnowledge());
        publishMappedUiKnowledge(output.curatedKnowledge().knowledge(), state);
        putArtifact(state, "ui.mapped.raw.page.count", String.valueOf(output.rawKnowledge().knowledge().pages().size()));
        putArtifact(state, "ui.mapped.curated.page.count", String.valueOf(output.curatedKnowledge().knowledge().pages().size()));
        putArtifact(state, "ui.mapped.curated.excluded.evidence.count",
                String.valueOf(output.curatedKnowledge().excludedEvidence().size()));
        putArtifact(state, "ui.mapped.curated.confidence",
                String.format(java.util.Locale.ROOT, "%.2f", output.curatedKnowledge().confidence()));
        addFinding(state, "Mapped UI knowledge curated for persistence/prompt use; excluded evidence: "
                + output.curatedKnowledge().excludedEvidence().size());
    }

    public void publishFlowScopedKnowledgePackage(FlowScopedKnowledgePackage knowledgePackage, WorkflowState state) {
        if (state == null || knowledgePackage == null) {
            return;
        }
        state.setFlowScopedKnowledgePackage(knowledgePackage);
        putArtifact(state, "flow.scope.page.count", String.valueOf(knowledgePackage.mappedUiKnowledge().pages().size()));
        putArtifact(state, "flow.scope.interaction.count",
                String.valueOf(knowledgePackage.canonicalInteractionModel().interactions().size()));
        putArtifact(state, "flow.scope.vector.match.count",
                String.valueOf(knowledgePackage.retrievalContext().vectorMatches().size()));
        putArtifact(state, "flow.scope.graph.match.count",
                String.valueOf(knowledgePackage.retrievalContext().graphMatches().size()));
        addFinding(state, "Flow-scoped knowledge package prepared for AI context");
        aiArtifactPublisher.writeDebugJson(
                state,
                "flow-scoped-knowledge",
                "flow-scoped-knowledge-package.json",
                knowledgePackage
        );
    }

    public void publishRefreshedFlowScopedKnowledgePackage(FlowScopedKnowledgePackage knowledgePackage, WorkflowState state) {
        if (state == null || knowledgePackage == null) {
            return;
        }
        state.setFlowScopedKnowledgePackage(knowledgePackage);
        putArtifact(state, "flow.scoped.knowledge.refreshed", "true");
        addFinding(state, "Flow-scoped knowledge refreshed after UI knowledge persistence");
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
        putArtifact(state, "page.knowledge.cache.retrieval.mode", retrievalMode == null ? "" : retrievalMode);
        putArtifact(state, "page.knowledge.cache.hit.count", String.valueOf(result.hitCount()));
        putArtifact(state, "page.knowledge.cache.miss.count", String.valueOf(result.missCount()));
        aiArtifactPublisher.writeJson(
                state,
                "enrichment",
                "page-knowledge-cache-lookup.json",
                result
        );
        addFinding(state, "Page knowledge cache lookup completed: hits="
                + result.hitCount() + ", misses=" + result.missCount());
    }

    public void publishPageModelEnrichment(PageModelEnrichmentOutput output, WorkflowState state) {
        if (state == null || output == null) {
            return;
        }
        state.setPageModelEnrichments(output.records());
        state.setEnrichedMappedUiKnowledge(output.enrichedMappedUiKnowledge());
        putArtifact(state, "page.enrichment.page.count", String.valueOf(output.records().size()));
        putArtifact(state, "page.enrichment.cache.hit.count", String.valueOf(output.cachedRecords().size()));
        putArtifact(state, "page.enrichment.generated.count", String.valueOf(output.generatedRecords().size()));
        putArtifact(state, "llm.schema.page.model.enrichment.version", LlmOutputSchemaVersion.PAGE_MODEL_ENRICHMENT_RECORD);
        putArtifact(state, "page.enrichment.openai.count", String.valueOf(output.openAiSuccesses()));
        putArtifact(state, "page.enrichment.openai.attempt.count", String.valueOf(output.openAiAttempts()));
        putArtifact(state, "page.enrichment.openai.success.count", String.valueOf(output.openAiSuccesses()));
        putArtifact(state, "page.enrichment.openai.failure.count", String.valueOf(output.openAiFailures()));
        putArtifact(state, "page.enrichment.openai.fallback.count", String.valueOf(output.openAiFallbacks()));
        putArtifact(state, "page.enrichment.openai.prompt.chars", String.valueOf(output.promptChars()));
        putArtifact(state, "page.enrichment.openai.response.chars", String.valueOf(output.responseChars()));
        putArtifact(state, "page.enrichment.openai.input.tokens", String.valueOf(output.actualInputTokens()));
        putArtifact(state, "page.enrichment.openai.output.tokens", String.valueOf(output.actualOutputTokens()));
        putArtifact(state, "page.enrichment.openai.total.tokens", String.valueOf(output.actualTotalTokens()));
        putArtifact(state, "page.enrichment.failures", String.valueOf(output.failures().size()));
        aiArtifactPublisher.writeJson(state, "enrichment", "page-model-enrichments.json", output.records());
        Map<String, Object> report = new java.util.LinkedHashMap<>();
        report.put("records", output.records().size());
        report.put("cacheHits", output.cachedRecords().size());
        report.put("generatedRecords", output.generatedRecords().size());
        report.put("openAiRecords", output.openAiSuccesses());
        report.put("openAiAttempts", output.openAiAttempts());
        report.put("openAiSuccesses", output.openAiSuccesses());
        report.put("openAiFailures", output.openAiFailures());
        report.put("openAiFallbacks", output.openAiFallbacks());
        report.put("promptChars", output.promptChars());
        report.put("responseChars", output.responseChars());
        report.put("inputTokens", output.actualInputTokens());
        report.put("outputTokens", output.actualOutputTokens());
        report.put("totalTokens", output.actualTotalTokens());
        report.put("failures", output.failures());
        aiArtifactPublisher.writeJson(state, "enrichment", "page-model-enrichment-report.json", report);
        aiArtifactPublisher.writeJson(
                state,
                "enrichment",
                "page-model-enrichment-failures.json",
                output.failureDetails()
        );
        addFinding(state, "PageModel enrichment prepared "
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
            putArtifact(state, "ui.knowledge.run.id", runMetadata.runId());
            putArtifact(state, "ui.knowledge.app.id", runMetadata.appId());
            putArtifact(state, "ui.knowledge.schema.version", runMetadata.schemaVersion());
            putArtifact(state, "ui.knowledge.requirement.set.hash", runMetadata.requirementSetHash());
            putArtifact(state, "ui.knowledge.base.url.hash", runMetadata.baseUrlHash());
            putArtifact(state, "ui.knowledge.discovery.session.id", runMetadata.discoverySessionId());
        }
        int executedTargets = 0;
        for (PageKnowledgeWriteResult result : results == null ? List.<PageKnowledgeWriteResult>of() : results) {
            putArtifact(state, "ui.knowledge." + result.target() + ".executed", String.valueOf(result.executed()));
            putArtifact(state, "ui.knowledge." + result.target() + ".node.count", String.valueOf(result.nodeCount()));
            putArtifact(state, "ui.knowledge." + result.target() + ".edge.count", String.valueOf(result.edgeCount()));
            putArtifact(state, "ui.knowledge." + result.target() + ".document.count", String.valueOf(result.documentCount()));
            if (!result.details().isBlank()) {
                addFinding(state, result.target().toUpperCase() + ": " + result.details());
            }
            if (result.executed()) {
                executedTargets++;
            }
        }
        putArtifact(state, "ui.knowledge.persistence.completed", "true");
        putArtifact(state, "ui.knowledge.persistence.executed.target.count", String.valueOf(executedTargets));
    }

    public void publishAssertionContracts(List<AssertionContract> contracts, WorkflowState state) {
        if (state == null || contracts == null) {
            return;
        }
        state.setAssertionContracts(contracts);
        putArtifact(state, "assertion.contract.count", String.valueOf(contracts.size()));
        aiArtifactPublisher.writeJson(state, "expectations", "assertion-contracts.json", contracts);
        addFinding(state, "Assertion contracts prepared " + contracts.size() + " typed assertion contract(s)");
    }

    public void publishApiGenerationResult(ApiGenerationResult result, WorkflowState state) {
        if (state == null || result == null) {
            return;
        }
        putArtifacts(state, result.artifacts());
        aiArtifactPublisher.writeJson(state, "api", "api-endpoint-bundle.json", result.endpoints());
        aiArtifactPublisher.writeJson(state, "api", "canonical-api-test-cases.json", result.testCases());
        aiArtifactPublisher.writeJson(state, "api", "api-generation-spec.json", result.generationSpec());
        aiArtifactPublisher.writeJson(state, "api", "api-quality-report.json", result.qualityReport());
        aiArtifactPublisher.writeJson(state, "api", "generated-source-files.json", result.sourceFiles());
        if (apiPreviewArtifactsEnabled()) {
            for (GeneratedSourceFile sourceFile : result.sourceFiles()) {
                aiArtifactPublisher.writeText(
                    state,
                    "api-preview",
                    "preview-" + sourceFile.className() + ".java",
                    sourceFile.content()
                );
            }
        }
        if (result.qualityReport() != null && !result.qualityReport().hasBlockingIssues()) {
            state.setApiSourceFiles(result.sourceFiles().stream()
                    .filter(sourceFile -> sourceFile.relativePath().startsWith("src/main/java/"))
                    .toList());
            state.setApiTestFiles(result.sourceFiles().stream()
                    .filter(sourceFile -> sourceFile.relativePath().startsWith("src/test/java/"))
                    .toList());
            putArtifact(state, "api.generated.source.persistable", "true");
        } else {
            state.setApiSourceFiles(List.of());
            state.setApiTestFiles(List.of());
            putArtifact(state, "api.generated.source.persistable", "false");
        }
        addFinding(state, "API generation prepared "
                + result.endpoints().endpoints().size() + " endpoint(s), "
                + result.generationSpec().clientSpecs().size() + " client spec(s), "
                + result.sourceFiles().size() + " source preview file(s)");
    }

    public void publishAiContextPackage(AiContextPackage contextPackage, WorkflowState state) {
        if (state == null || contextPackage == null) {
            return;
        }
        state.setAiContextPackage(contextPackage);
        state.setPromptUiEvidence(contextPackage.promptUiEvidence());
        putArtifact(state, "ai.context.ready", "true");
        putArtifact(state, "prompt.ui.evidence.locator.count",
                String.valueOf(contextPackage.promptUiEvidence().requiredLocators().size()));
        putArtifact(state, "prompt.ui.evidence.confirmed.locator.count",
                String.valueOf(contextPackage.promptUiEvidence().requiredLocators().size()));
        putArtifact(state, "prompt.ui.evidence.candidate.locator.count",
                String.valueOf(contextPackage.promptUiEvidence().candidateLocators().size()));
        putArtifact(state, "prompt.ui.evidence.fallback.locator.count",
                String.valueOf(contextPackage.promptUiEvidence().fallbackLocators().size()));
        putArtifact(state, "prompt.ui.evidence.excluded.count",
                String.valueOf(contextPackage.promptUiEvidence().excludedEvidence().size()));
        putArtifact(state, "ai.context.db.stable.locator.count",
                String.valueOf(contextPackage.dbStableLocatorEvidence().size()));
        putArtifact(state, 
                "ai.context.canonical.interactions",
                String.valueOf(contextPackage.canonicalInteractionModel().interactions().size())
        );
        putArtifact(state, 
                "ai.context.retrieval.vector.matches",
                String.valueOf(contextPackage.retrievalContext().vectorMatches().size())
        );
        putArtifact(state, 
                "ai.context.retrieval.graph.matches",
                String.valueOf(contextPackage.retrievalContext().graphMatches().size())
        );
        addFinding(state, "AI context package assembled from typed pipeline input");
        aiArtifactPublisher.writeDebugJson(
                state,
                "context",
                "ai-context-package.json",
                contextPackage
        );
    }

    public void publishAiPageObjectSpecs(List<AiPageObjectSpec> specs, WorkflowState state) {
        if (state == null || specs == null) {
            return;
        }
        state.setAiPageObjectSpecs(specs);
        putArtifact(state, "ai.page.object.spec.count", String.valueOf(specs.size()));
    }

    public void publishAiPageObjectGenerationResult(AiPageObjectGenerationResult result, WorkflowState state) {
        if (state == null || result == null) {
            return;
        }
        state.setPomContractSpecs(result.contracts());
        state.setAiPageObjectSpecs(result.specs());
        result.artifactFiles().forEach(file -> aiArtifactPublisher.register(state, file));
        putArtifacts(state, result.artifacts());
        result.findings().forEach(finding -> addFinding(state, finding));
        putArtifact(state, "pom.contract.spec.count", String.valueOf(result.contracts().size()));
        putArtifact(state, "ai.page.object.spec.count", String.valueOf(result.specs().size()));
    }

    public void publishAiUiTestGenerationResult(AiUiTestGenerationResult result, WorkflowState state) {
        if (state == null || result == null) {
            return;
        }
        state.setAiUiTestSpecs(result.specs());
        result.artifactFiles().forEach(file -> aiArtifactPublisher.register(state, file));
        putArtifacts(state, result.artifacts());
        result.findings().forEach(finding -> addFinding(state, finding));
        putArtifact(state, "ai.ui.test.spec.count", String.valueOf(result.specs().size()));
    }

    public void publishGeneratedUiContractValidation(
            GeneratedUiContractValidationResult result,
            WorkflowState state
    ) {
        if (state == null || result == null) {
            return;
        }
        state.setGeneratedUiContractValidationResult(result);
        putArtifact(state, "generated.ui.contract.validation.status", result.status().name());
        putArtifact(state, "generated.ui.contract.validation.summary", result.summary());
        addFinding(state, result.summary());

        if (result.isFailed()) {
            failRun(state, "Generated UI contract validation failed:\n- "
                    + String.join("\n- ", result.violations()));
        }
    }

    public void publishGeneratedCodeValidation(
            GeneratedCodeValidationResult result,
            WorkflowState state
    ) {
        if (state == null || result == null) {
            return;
        }
        putArtifact(state, "generated.code.validation.status", result.status().name());
        putArtifact(state, "generated.code.validation.summary", result.summary());
        addFinding(state, result.summary());
        state.setGeneratedCodeValidationResult(result);

        if (result.isFailed()) {
            failRun(state, "Generated code validation failed:\n" + result.compilerOutput());
        }
    }

    public void publishGeneratedCodeReview(
            GeneratedCodeReviewReport report,
            WorkflowState state
    ) {
        if (state == null || report == null) {
            return;
        }
        state.setGeneratedCodeReviewReport(report);
        putArtifact(state, "generated.code.review.findings", String.valueOf(report.totalFindings()));
        putArtifact(state, "generated.code.review.summary", report.summary());
        addFinding(state, report.summary());
    }

    public void publishApiGeneratedSourcePersistence(List<String> writtenFiles, WorkflowState state) {
        if (state == null) {
            return;
        }
        List<String> files = writtenFiles == null ? List.of() : writtenFiles;
        files.forEach(state::addWrittenFile);
        putArtifact(state, "api.generated.source.persisted", "true");
        putArtifact(state, "api.generated.source.file.written", String.valueOf(files.size()));
        addFinding(state, "API generated source persisted: " + files.size());
    }

    public void publishAiPageObjectFiles(List<GeneratedSourceFile> files, WorkflowState state) {
        if (state == null) {
            return;
        }
        List<GeneratedSourceFile> generated = files == null ? List.of() : files;
        if (generated.isEmpty()) {
            failRun(state, "Pure AI mode did not produce any page object files");
            return;
        }
        state.setPageObjectFiles(generated);
        putArtifact(state, "ui.page.objects.count", String.valueOf(generated.size()));
        putArtifact(state, "ui.page.objects.ai.override.count", String.valueOf(generated.size()));
        addFinding(state, "Pure AI page objects generated: " + generated.size());
    }

    public void publishAiUiTestFiles(List<GeneratedSourceFile> files, WorkflowState state) {
        if (state == null) {
            return;
        }
        List<GeneratedSourceFile> generated = files == null ? List.of() : files;
        if (generated.isEmpty()) {
            failRun(state, "Pure AI mode did not produce any UI test files");
            return;
        }
        state.setUiTestFiles(generated);
        putArtifact(state, "ui.test.count", String.valueOf(generated.size()));
        putArtifact(state, "ui.test.ai.override.count", String.valueOf(generated.size()));
        addFinding(state, "Pure AI UI test files generated: " + generated.size());
    }

    public void publishDeterministicUiTestFiles(List<GeneratedSourceFile> files, WorkflowState state) {
        if (state == null) {
            return;
        }
        List<GeneratedSourceFile> generated = files == null ? List.of() : files;
        if (generated.isEmpty()) {
            failRun(state, "Deterministic test writer did not produce any UI test files");
            return;
        }
        state.setUiTestFiles(generated);
        putArtifact(state, "ui.test.count", String.valueOf(generated.size()));
        putArtifact(state, "ui.test.writer", "deterministic-testng-v1");
        addFinding(state, "Deterministic TestNG files generated: " + generated.size());
    }

    public void publishRepositoryIntelligenceEnrichment(
            RepositoryIntelligenceEnrichmentResult result,
            WorkflowState state
    ) {
        if (state == null || result == null) {
            return;
        }
        putArtifacts(state, result.artifacts());
        result.artifactFiles().forEach(file -> aiArtifactPublisher.register(state, file));
        result.findings().forEach(finding -> addFinding(state, finding));
    }

    public void publishUiDiscoveryArtifactPersistence(
            UiDiscoveryArtifactPersistenceResult result,
            WorkflowState state
    ) {
        if (state == null || result == null) {
            return;
        }
        result.writtenFiles().forEach(state::addDiscoveryArtifactFile);
        putArtifacts(state, result.artifacts());
        result.findings().forEach(finding -> addFinding(state, finding));
    }

    private void putArtifact(WorkflowState state, String key, String value) {
        if (state == null || key == null || key.isBlank() || value == null) {
            return;
        }
        state.addArtifact(key, value);
    }

    private void putArtifacts(WorkflowState state, Map<String, String> artifacts) {
        if (state == null || artifacts == null || artifacts.isEmpty()) {
            return;
        }
        artifacts.forEach((key, value) -> putArtifact(state, key, value));
    }

    private void addFinding(WorkflowState state, String finding) {
        if (state == null || finding == null || finding.isBlank()) {
            return;
        }
        state.addFinding(finding);
    }

    private void failRun(WorkflowState state, String reason) {
        if (state == null || reason == null || reason.isBlank()) {
            return;
        }
        state.fail(reason);
    }

    private boolean apiPreviewArtifactsEnabled() {
        String explicit = firstNonBlank(
                System.getProperty("api.preview.enabled"),
                System.getenv("API_PREVIEW_ENABLED")
        );
        if (!explicit.isBlank()) {
            return Boolean.parseBoolean(explicit);
        }
        String apiEnabled = firstNonBlank(
                System.getProperty("api.enabled"),
                System.getenv("API_ENABLED")
        );
        return !apiEnabled.isBlank() && Boolean.parseBoolean(apiEnabled);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim().toLowerCase(Locale.ROOT);
            }
        }
        return "";
    }
}

