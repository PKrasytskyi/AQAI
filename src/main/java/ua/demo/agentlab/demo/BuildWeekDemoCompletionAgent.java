package ua.demo.agentlab.demo;

import ua.demo.agentlab.ai.openai.PropertiesOpenAiRuntimeConfig;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.artifactreuse.metrics.ArtifactReuseRunMetrics;
import ua.demo.agentlab.artifactreuse.metrics.ArtifactReuseTypeMetrics;
import ua.demo.agentlab.config.RuntimeProperties;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.persistence.GeneratedSourceKind;
import ua.demo.agentlab.persistence.GeneratedSourceManifest;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.review.ReviewSeverity;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.discovery.catalog.ConfirmedUiCatalog;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractBundle;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.execution.GeneratedTestExecutionResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Terminal manifest acceptance gate. It observes pipeline decisions and never changes UI evidence. */
public final class BuildWeekDemoCompletionAgent implements WorkflowAgent,
        PipelineAgent<BuildWeekDemoCompletionAgent.Input, BuildWeekDemoSummary> {

    private final RuntimeProperties properties;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();
    private final BuildWeekRunSummaryProjector runSummaryProjector = new BuildWeekRunSummaryProjector();

    public BuildWeekDemoCompletionAgent() {
        this(new RuntimeProperties());
    }

    BuildWeekDemoCompletionAgent(RuntimeProperties properties) {
        this.properties = properties == null ? new RuntimeProperties() : properties;
    }

    @Override
    public String name() {
        return "build-week-demo-completion-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE,
                WorkflowArtifact.CONFIRMED_UI_CATALOG,
                WorkflowArtifact.POM_CONTRACT_SPECS,
                WorkflowArtifact.UI_TEST_CONTRACT_BUNDLE,
                WorkflowArtifact.GENERATED_SOURCE_MANIFEST,
                WorkflowArtifact.COMPILE_RESULT,
                WorkflowArtifact.REVIEW_RESULT,
                WorkflowArtifact.GENERATED_UI_SMOKE_RESULT,
                WorkflowArtifact.GENERATED_TEST_EXECUTION_RESULT,
                WorkflowArtifact.ARTIFACT_REUSE_METRICS,
                WorkflowArtifact.RUNTIME_FEEDBACK_DB_UPDATE
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.BUILD_WEEK_DEMO_SUMMARY);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.GENERATED_TEST_EXECUTION_RESULT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.BUILD_WEEK_DEMO_SUMMARY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Input inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new Input(
                manifest(),
                store.require(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE),
                store.require(WorkflowArtifact.CONFIRMED_UI_CATALOG),
                (List<PomContractSpec>) store.require(WorkflowArtifact.POM_CONTRACT_SPECS),
                store.require(WorkflowArtifact.UI_TEST_CONTRACT_BUNDLE),
                store.require(WorkflowArtifact.GENERATED_SOURCE_MANIFEST),
                store.require(WorkflowArtifact.COMPILE_RESULT),
                store.require(WorkflowArtifact.REVIEW_RESULT),
                store.require(WorkflowArtifact.GENERATED_UI_SMOKE_RESULT),
                store.require(WorkflowArtifact.GENERATED_TEST_EXECUTION_RESULT),
                store.require(WorkflowArtifact.ARTIFACT_REUSE_METRICS),
                state == null ? Map.of() : state.getArtifacts()
        );
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        if (!properties.readBoolean("demo.execution.active", "false")) {
            return false;
        }
        return PipelineAgent.super.supports(store, state);
    }

    @Override
    public BuildWeekDemoSummary execute(Input input, WorkflowRunEnvelope run) {
        List<String> issues = validate(input);
        int pageObjects = count(input.sources(), GeneratedSourceKind.PAGE_OBJECT);
        int tests = count(input.sources(), GeneratedSourceKind.UI_TEST);
        int enrichmentCalls = integer(input.artifacts(), "page.enrichment.openai.attempt.count",
                integer(input.artifacts(), "page.enrichment.openai.count"));
        int enrichmentCacheHits = integer(input.artifacts(), "page.enrichment.cache.hit.count");
        int enrichmentFailures = integer(input.artifacts(), "page.enrichment.openai.failure.count");
        int pomCalls = input.reuseMetrics().llmCallsExecuted();
        int stablePomReuse = reuseHits(input.reuseMetrics(), "POM_CONTRACT");
        int avoidedCalls = enrichmentCacheHits + stablePomReuse;
        String aiStatus = aiReasoningStatus(enrichmentCalls + pomCalls, avoidedCalls, enrichmentFailures);
        int criticalReviewFindings = (int) input.review().findings().stream()
                .filter(finding -> finding.severity() == ReviewSeverity.CRITICAL)
                .count();
        int promptBlockers = integer(input.artifacts(), "ai.run.quality.prompt.blocking.issues");
        int terminalIssuesWithoutReview = Math.max(0, issues.size() - (criticalReviewFindings > 0 ? 1 : 0));
        int blockingIssues = promptBlockers + criticalReviewFindings + terminalIssuesWithoutReview;
        int qualityScore = integer(input.artifacts(), "ai.run.quality.score");
        if (!issues.isEmpty()) qualityScore = Math.min(qualityScore, 85);
        return new BuildWeekDemoSummary(
                BuildWeekDemoSummary.SCHEMA_VERSION,
                input.manifest().demoId(),
                run.runMetadata().runId(),
                issues.isEmpty() ? "PASSED" : "FAILED",
                runMode(aiStatus),
                input.artifacts().getOrDefault("project.profile.name", input.manifest().demoId()),
                new PropertiesOpenAiRuntimeConfig().model(),
                aiStatus,
                enrichmentCalls,
                enrichmentCacheHits,
                enrichmentFailures,
                pomCalls,
                stablePomReuse,
                input.reuseMetrics().llmCallsSkipped(),
                avoidedCalls,
                bool(input.artifacts(), "ui.knowledge.retrieval.neo4j.hit"),
                bool(input.artifacts(), "ui.knowledge.retrieval.qdrant.hit"),
                bool(input.artifacts(), "ui.knowledge.retrieval.stable.cache.used")
                        || input.reuseMetrics().stableLocatorReuse() > 0
                        || avoidedCalls > 0,
                0,
                input.cases().testCases().size(),
                input.catalog().pages().size(),
                input.catalog().complete() ? "VERIFIED" : "INCOMPLETE",
                qualityScore,
                input.pomContracts().stream().mapToInt(contract -> contract.coverageGaps().size()).sum(),
                blockingIssues,
                input.pomContracts().size(),
                pageObjects,
                input.testContracts().contracts().size(),
                tests,
                input.compile().status().name(),
                input.review().findings().size(),
                input.sourceSmoke().status().name(),
                input.artifacts().getOrDefault("generated.ui.live.smoke.status", "MISSING"),
                input.execution().status().name(),
                input.execution().total(),
                input.execution().passed(),
                input.execution().failed(),
                input.artifacts().getOrDefault("runtime.feedback.db.update.details", "not-recorded"),
                List.of("page semantic enrichment", "POM contract planning"),
                List.of(
                        "requirements normalization and canonical scenario planning",
                        "UI evidence verification and promotion",
                        "Page Object Java generation",
                        "TestNG Java generation",
                        "compile, review, smoke, and generated test execution"
                ),
                issues
        );
    }

    @Override
    public void applyOutput(BuildWeekDemoSummary output, WorkflowState state) {
        artifactPublisher.writeJson(state, "quality", "build-week-demo-summary.json", output);
        artifactPublisher.writeText(state, "quality", "build-week-demo-summary.md", markdown(output));
        runSummaryProjector.project(output);
        state.addArtifact("build.week.demo.status", output.status());
        state.addArtifact("build.week.demo.summary.file", "target/ai-run/quality/build-week-demo-summary.json");
        state.addArtifact("build.week.demo.model", output.openAiModel());
        state.addArtifact("build.week.demo.run.mode", output.runMode());
        state.addArtifact("build.week.demo.project.name", output.projectName());
        state.addArtifact("build.week.demo.ai.status", output.aiReasoningStatus());
        state.addArtifact("build.week.demo.scenarios", String.valueOf(output.canonicalScenarios()));
        state.addArtifact("build.week.demo.catalog.pages", String.valueOf(output.confirmedCatalogPages()));
        state.addArtifact("build.week.demo.pom.count", String.valueOf(output.generatedPageObjects()));
        state.addArtifact("build.week.demo.test.count", String.valueOf(output.generatedTests()));
        state.addArtifact("build.week.demo.page.enrichment.calls", String.valueOf(output.pageEnrichmentLlmCalls()));
        state.addArtifact("build.week.demo.page.enrichment.cache.hits", String.valueOf(output.pageEnrichmentCacheHits()));
        state.addArtifact("build.week.demo.page.enrichment.failures", String.valueOf(output.pageEnrichmentLlmFailures()));
        state.addArtifact("build.week.demo.pom.llm.calls", String.valueOf(output.pomContractLlmCalls()));
        state.addArtifact("build.week.demo.pom.reused", String.valueOf(output.stablePomContractsReused()));
        state.addArtifact("build.week.demo.llm.calls.avoided", String.valueOf(output.avoidedLlmCalls()));
        state.addArtifact("build.week.demo.neo4j.hit", String.valueOf(output.neo4jHit()));
        state.addArtifact("build.week.demo.qdrant.hit", String.valueOf(output.qdrantHit()));
        state.addArtifact("build.week.demo.stable.evidence.reused", String.valueOf(output.stableEvidenceReused()));
        state.addArtifact("build.week.demo.locator.evidence.status", output.locatorEvidenceStatus());
        state.addArtifact("build.week.demo.quality.score", String.valueOf(output.qualityScore()));
        state.addArtifact("build.week.demo.coverage.gaps", String.valueOf(output.coverageGapCount()));
        state.addArtifact("build.week.demo.blocking.issues", String.valueOf(output.blockingIssueCount()));
        state.addArtifact("build.week.demo.compile.status", output.compileStatus());
        state.addArtifact("build.week.demo.review.findings", String.valueOf(output.reviewFindings()));
        state.addArtifact("build.week.demo.source.smoke.status", output.generatedSourceSmokeStatus());
        state.addArtifact("build.week.demo.live.smoke.status", output.liveSmokeStatus());
        state.addArtifact("build.week.demo.execution.status", output.generatedTestExecutionStatus());
        state.addArtifact("build.week.demo.tests.executed", String.valueOf(output.executedTests()));
        state.addArtifact("build.week.demo.tests.passed", String.valueOf(output.passedTests()));
        state.addArtifact("build.week.demo.tests.failed", String.valueOf(output.failedTests()));
        state.addFinding("Build Week demo: " + output.status() + " (" + output.passedTests()
                + "/" + output.executedTests() + " generated tests passed)");
        if (!output.passed()) {
            state.fail("Build Week demo acceptance failed: " + String.join("; ", output.issues()));
        }
    }

    private List<String> validate(Input input) {
        List<String> issues = new ArrayList<>();
        compare("scenario IDs", new LinkedHashSet<>(input.manifest().expectedScenarioIds()),
                input.cases().testCases().stream().map(testCase -> testCase.id()).collect(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new)), issues);
        compare("POM contract names", new LinkedHashSet<>(input.manifest().expectedPomNames()),
                input.pomContracts().stream().map(contract -> contract.page().name()).collect(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new)), issues);
        compare("generated Page Objects", new LinkedHashSet<>(input.manifest().expectedPomNames()),
                classes(input.sources(), GeneratedSourceKind.PAGE_OBJECT), issues);
        compare("UI test contracts", new LinkedHashSet<>(input.manifest().expectedGeneratedTests()),
                input.testContracts().contracts().stream().map(contract -> contract.className()).collect(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new)), issues);
        compare("generated TestNG classes", new LinkedHashSet<>(input.manifest().expectedGeneratedTests()),
                classes(input.sources(), GeneratedSourceKind.UI_TEST), issues);
        if (!input.catalog().complete()) issues.add("confirmed UI catalog is incomplete");
        if (!input.compile().isPassed()) issues.add("generated code compile status is " + input.compile().status());
        long blockingReviewFindings = input.review().findings().stream()
                .filter(finding -> finding.severity() == ReviewSeverity.CRITICAL)
                .count();
        if (blockingReviewFindings > 0) {
            issues.add("generated code review contains " + blockingReviewFindings + " critical finding(s)");
        }
        if (!input.sourceSmoke().passed()) issues.add("generated source smoke status is " + input.sourceSmoke().status());
        String liveStatus = input.artifacts().getOrDefault("generated.ui.live.smoke.status", "MISSING");
        if (!"PASSED".equalsIgnoreCase(liveStatus)) issues.add("live UI smoke status is " + liveStatus);
        if (!input.execution().successful()) {
            issues.add("generated TestNG execution status is " + input.execution().status());
        }
        if (input.execution().total() != input.manifest().expectedGeneratedTests().size()) {
            issues.add("executed test count " + input.execution().total() + " does not match manifest count "
                    + input.manifest().expectedGeneratedTests().size());
        }
        return List.copyOf(issues);
    }

    private void compare(String label, Set<String> expected, Set<String> actual, List<String> issues) {
        if (!expected.equals(actual)) {
            issues.add(label + " mismatch: expected=" + expected + ", actual=" + actual);
        }
    }

    private int count(GeneratedSourceManifest manifest, GeneratedSourceKind kind) {
        return (int) manifest.files().stream().filter(entry -> entry.kind() == kind).count();
    }

    private Set<String> classes(GeneratedSourceManifest manifest, GeneratedSourceKind kind) {
        return manifest.files().stream()
                .filter(entry -> entry.kind() == kind)
                .map(entry -> entry.className())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private int integer(Map<String, String> values, String key) {
        return integer(values, key, 0);
    }

    private int integer(Map<String, String> values, String key, int fallback) {
        try {
            return Integer.parseInt(values.getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean bool(Map<String, String> values, String key) {
        return Boolean.parseBoolean(values.getOrDefault(key, "false"));
    }

    private int reuseHits(ArtifactReuseRunMetrics metrics, String artifactType) {
        ArtifactReuseTypeMetrics type = metrics.reuseByArtifactType().get(artifactType);
        return type == null ? 0 : type.hits();
    }

    private String aiReasoningStatus(int calls, int reused, int failures) {
        if (failures > 0) return "FAILED";
        if (calls > 0 && reused > 0) return "PARTIAL_REUSE";
        if (calls > 0) return "EXECUTED";
        if (reused > 0) return "REUSED";
        return "NOT_USED";
    }

    private String runMode(String aiStatus) {
        return switch (aiStatus) {
            case "EXECUTED" -> "COLD-DISCOVERY";
            case "REUSED" -> "KNOWLEDGE-REUSE";
            case "PARTIAL_REUSE" -> "HYBRID-REUSE";
            case "FAILED" -> "FAILED";
            default -> "AI-DISABLED";
        };
    }

    private DemoManifest manifest() {
        String path = properties.readValue("demo.manifest.file", "");
        if (path.isBlank()) {
            throw new IllegalStateException("Demo manifest is required for Build Week completion gate");
        }
        return new DemoManifestLoader().load(Path.of(path));
    }

    private String markdown(BuildWeekDemoSummary summary) {
        return """
                # Build Week Demo Summary

                | Metric | Result |
                |---|---:|
                | Status | %s |
                | Run mode | %s |
                | Project | %s |
                | Model | %s |
                | AI reasoning | %s |
                | Page enrichment calls / cache hits | %d / %d |
                | POM planning calls / stable reuse | %d / %d |
                | Avoided LLM calls | %d |
                | Neo4j / Qdrant | %s / %s |
                | Quality score | %d / 100 |
                | Coverage gaps | %d |
                | Blocking issues | %d |
                | Canonical scenarios | %d |
                | POM contracts / Page Objects | %d / %d |
                | Test contracts / generated tests | %d / %d |
                | Compile | %s |
                | Review findings | %d |
                | Generated source smoke | %s |
                | Live smoke | %s |
                | Generated tests | %d/%d passed |
                | Runtime feedback DB | %s |
                | Test generation LLM calls | %d |

                Issues: %s
                """.formatted(
                summary.status(), summary.runMode(), summary.projectName(), summary.openAiModel(), summary.aiReasoningStatus(),
                summary.pageEnrichmentLlmCalls(), summary.pageEnrichmentCacheHits(),
                summary.pomContractLlmCalls(), summary.stablePomContractsReused(),
                summary.avoidedLlmCalls(), summary.neo4jHit() ? "HIT" : "MISS",
                summary.qdrantHit() ? "HIT" : "MISS", summary.qualityScore(),
                summary.coverageGapCount(), summary.blockingIssueCount(), summary.canonicalScenarios(),
                summary.pomContracts(), summary.generatedPageObjects(), summary.uiTestContracts(),
                summary.generatedTests(), summary.compileStatus(), summary.reviewFindings(), summary.generatedSourceSmokeStatus(),
                summary.liveSmokeStatus(), summary.passedTests(), summary.executedTests(),
                summary.runtimeFeedbackDbStatus(), summary.testGenerationLlmCalls(),
                summary.issues().isEmpty() ? "none" : String.join("; ", summary.issues())
        );
    }

    public record Input(
            DemoManifest manifest,
            CanonicalTestCaseBundle cases,
            ConfirmedUiCatalog catalog,
            List<PomContractSpec> pomContracts,
            UiTestContractBundle testContracts,
            GeneratedSourceManifest sources,
            GeneratedCodeValidationResult compile,
            GeneratedCodeReviewReport review,
            GeneratedUiSmokeResult sourceSmoke,
            GeneratedTestExecutionResult execution,
            ArtifactReuseRunMetrics reuseMetrics,
            Map<String, String> artifacts
    ) {
        public Input {
            pomContracts = pomContracts == null ? List.of() : List.copyOf(pomContracts);
            artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
        }
    }
}
