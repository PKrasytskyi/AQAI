package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.AiContextScope;
import ua.demo.agentlab.ai.context.AiContextScopeResolver;
import ua.demo.agentlab.ai.context.TargetAwareContextSlicer;
import ua.demo.agentlab.ai.debug.AiPromptTraceArtifact;
import ua.demo.agentlab.ai.debug.AiPromptTraceRecorder;
import ua.demo.agentlab.ai.debug.AiRunArtifactWriter;
import ua.demo.agentlab.ai.openai.OpenAiRuntimeConfig;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;
import ua.demo.agentlab.ai.ui.prompt.AiUiTestPromptBuilder;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiUiTestSpecGenerator {

    private final OpenAiRuntimeConfig runtimeConfig;
    private final AiUiTestPromptBuilder promptBuilder;
    private final AiRunArtifactWriter artifactWriter;
    private final AiPromptTraceRecorder promptTraceRecorder;
    private final AiContextScopeResolver scopeResolver;
    private final TargetAwareContextSlicer contextSlicer;

    public AiUiTestSpecGenerator(OpenAiRuntimeConfig runtimeConfig) {
        if (runtimeConfig == null) {
            throw new IllegalArgumentException("runtimeConfig cannot be null");
        }
        this.runtimeConfig = runtimeConfig;
        this.promptBuilder = new AiUiTestPromptBuilder();
        this.artifactWriter = new AiRunArtifactWriter();
        this.promptTraceRecorder = new AiPromptTraceRecorder();
        this.scopeResolver = new AiContextScopeResolver();
        this.contextSlicer = new TargetAwareContextSlicer();
    }

    public AiUiTestGenerationResult generate(AiUiTestGenerationRequest request) {
        if (request == null || request.uiTestPlan() == null || request.contextPackage() == null) {
            return AiUiTestGenerationResult.empty();
        }
        List<String> artifactFiles = new ArrayList<>();
        Map<String, String> artifacts = new LinkedHashMap<>();
        List<String> findings = new ArrayList<>();
        try {
            AiContextPackage contextPackage = request.contextPackage();
            List<AiUiTestSpec> specs = new ArrayList<>();
            for (UiTestScenario scenario : request.uiTestPlan().scenarios()) {
                AiContextScope scenarioScope = scopeResolver.resolveForScenario(contextPackage, scenario);
                AiContextPackage scopedContext = contextSlicer.slice(contextPackage, scenarioScope);
                List<AiPageObjectSpec> scopedPageSpecs = filterPageSpecsForScenario(request.pageObjectSpecs(), scenario);
                AiUiTestSpec baselineSpec = request.baselineSpecs().stream()
                        .filter(spec -> scenario.id().equals(spec.scenarioId()))
                        .findFirst()
                        .orElse(null);
                String prompt = promptBuilder.buildForScenario(scopedContext, scenario, scopedPageSpecs, baselineSpec);
                String fileStem = sanitizeFileStem(scenario.id());
                AiPromptTraceArtifact traceArtifact = promptTraceRecorder.recordPromptArtifact(
                        "ui-test-spec",
                        fileStem,
                        "ui-test-spec",
                        "scenario:" + scenario.id(),
                        scenario.id(),
                        prompt,
                        buildPromptMetadata(scenario, scopedPageSpecs, baselineSpec, scopedContext)
                );
                artifactFiles.addAll(traceArtifact.artifactFiles());
            }
            artifacts.put("openai.ui.test.status", "llm-disabled-enrichment-only");
            artifacts.put("openai.ui.test.scoped.requests", String.valueOf(request.uiTestPlan().scenarios().size()));
            findings.add("OpenAI UI test generation is disabled; prompts were recorded for review only");
            return new AiUiTestGenerationResult(specs, artifactFiles, artifacts, findings);
        } catch (Exception exception) {
            artifactFiles.add(trackTextArtifact("ui-test-spec", "error.txt", exception.getMessage()));
            artifacts.put(
                    "openai.ui.test.status",
                    runtimeConfig.strict() ? "active-generation-failed-strict" : "active-generation-failed-no-output"
            );
            findings.add("OpenAI UI test generation failed: " + exception.getMessage());
            if (runtimeConfig.strict()) {
                throw new IllegalStateException("OpenAI strict mode rejected UI test spec generation", exception);
            }
            return new AiUiTestGenerationResult(List.of(), artifactFiles, artifacts, findings);
        }
    }

    private List<AiPageObjectSpec> filterPageSpecsForScenario(
            List<AiPageObjectSpec> pageObjectSpecs,
            UiTestScenario scenario
    ) {
        if (pageObjectSpecs == null || pageObjectSpecs.isEmpty()) {
            return List.of();
        }
        return pageObjectSpecs.stream()
                .filter(spec -> PageReferenceMatcher.matchesScenarioPage(scenario.pageName(), scenario.route(), spec.pageName())
                        || PageReferenceMatcher.matchesScenarioPage(scenario.sourcePageName(), scenario.sourceRoute(), spec.pageName())
                        || (scenario.prerequisite() != null
                        && PageReferenceMatcher.matchesScenarioPage(
                                scenario.prerequisite().sourcePageName(),
                                scenario.prerequisite().sourceRoute(),
                                spec.pageName()
                        )))
                .toList();
    }

    private String sanitizeFileStem(String value) {
        return value == null || value.isBlank()
                ? "scenario"
                : value.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private String trackTextArtifact(String stage, String fileName, String content) {
        return artifactWriter.writeText(stage, fileName, content).toString();
    }

    private Map<String, Object> buildPromptMetadata(
            UiTestScenario scenario,
            List<AiPageObjectSpec> scopedPageSpecs,
            AiUiTestSpec baselineSpec,
            AiContextPackage scopedContext
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("schemaVersion", LlmOutputSchemaVersion.AI_UI_TEST_SPEC);
        metadata.put("scenarioId", scenario == null ? "" : scenario.id());
        metadata.put("pageName", scenario == null ? "" : scenario.pageName());
        metadata.put("sourcePageName", scenario == null ? "" : scenario.sourcePageName());
        metadata.put("route", scenario == null ? "" : scenario.route());
        metadata.put("scopedPageSpecCount", scopedPageSpecs == null ? 0 : scopedPageSpecs.size());
        metadata.put("scopedPageSpecNames", scopedPageSpecs == null
                ? List.of()
                : scopedPageSpecs.stream().map(AiPageObjectSpec::pageName).distinct().toList());
        metadata.put("hasBaselineSpec", baselineSpec != null);
        metadata.put("mappedPageCount", scopedContext == null || scopedContext.mappedUiKnowledge() == null
                ? 0
                : scopedContext.mappedUiKnowledge().pages().size());
        metadata.put("canonicalCaseCount", scopedContext == null || scopedContext.canonicalTestCaseBundle() == null
                ? 0
                : scopedContext.canonicalTestCaseBundle().testCases().size());
        return metadata;
    }
}
