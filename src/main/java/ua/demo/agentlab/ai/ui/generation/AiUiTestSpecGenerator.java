package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.context.AiContextAssembler;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.AiContextScope;
import ua.demo.agentlab.ai.context.AiContextScopeResolver;
import ua.demo.agentlab.ai.context.TargetAwareContextSlicer;
import ua.demo.agentlab.ai.debug.AiPromptTraceRecorder;
import ua.demo.agentlab.ai.debug.AiRunArtifactWriter;
import ua.demo.agentlab.ai.openai.OpenAiClientFactory;
import ua.demo.agentlab.ai.openai.OpenAiRuntimeConfig;
import ua.demo.agentlab.ai.openai.OpenAiRuntimeConfigRagAdapter;
import ua.demo.agentlab.ai.openai.OpenAiRuntimeSettings;
import ua.demo.agentlab.ai.rag.openai.OpenAiEmptyOutputException;
import ua.demo.agentlab.ai.rag.openai.OpenAiResponseGenerationClient;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;
import ua.demo.agentlab.ai.ui.parser.AiUiTestSpecParser;
import ua.demo.agentlab.ai.ui.prompt.AiUiTestPromptBuilder;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AiUiTestSpecGenerator {

    private final OpenAiRuntimeConfig runtimeConfig;
    private final OpenAiResponseGenerationClient generationClient;
    private final AiContextAssembler contextAssembler;
    private final AiUiTestPromptBuilder promptBuilder;
    private final AiUiTestSpecParser parser;
    private final AiRunArtifactWriter artifactWriter;
    private final AiPromptTraceRecorder promptTraceRecorder;
    private final AiContextScopeResolver scopeResolver;
    private final TargetAwareContextSlicer contextSlicer;

    public AiUiTestSpecGenerator(OpenAiRuntimeConfig runtimeConfig, AiContextAssembler contextAssembler) {
        if (runtimeConfig == null) {
            throw new IllegalArgumentException("runtimeConfig cannot be null");
        }
        if (contextAssembler == null) {
            throw new IllegalArgumentException("contextAssembler cannot be null");
        }
        this.runtimeConfig = runtimeConfig;
        this.generationClient = new OpenAiResponseGenerationClient(new OpenAiRuntimeConfigRagAdapter(runtimeConfig));
        this.contextAssembler = contextAssembler;
        this.promptBuilder = new AiUiTestPromptBuilder();
        this.parser = new AiUiTestSpecParser();
        this.artifactWriter = new AiRunArtifactWriter();
        this.promptTraceRecorder = new AiPromptTraceRecorder();
        this.scopeResolver = new AiContextScopeResolver();
        this.contextSlicer = new TargetAwareContextSlicer();
    }

    public List<AiUiTestSpec> generate(
            WorkflowState state,
            List<AiPageObjectSpec> pageObjectSpecs,
            List<AiUiTestSpec> baselineSpecs
    ) {
        if (state == null || state.getUiTestPlan() == null) {
            return List.of();
        }
        try {
            AiContextPackage contextPackage = state.getAiContextPackage();
            if (contextPackage == null) {
                contextPackage = contextAssembler.assemble(state);
                state.setAiContextPackage(contextPackage);
            }
            List<AiUiTestSpec> specs = new ArrayList<>();
            for (UiTestScenario scenario : state.getUiTestPlan().scenarios()) {
                AiContextScope scenarioScope = scopeResolver.resolveForScenario(contextPackage, scenario);
                AiContextPackage scopedContext = contextSlicer.slice(contextPackage, scenarioScope);
                List<AiPageObjectSpec> scopedPageSpecs = filterPageSpecsForScenario(pageObjectSpecs, scenario);
                AiUiTestSpec baselineSpec = baselineSpecs == null ? null : baselineSpecs.stream()
                        .filter(spec -> scenario.id().equals(spec.scenarioId()))
                        .findFirst()
                        .orElse(null);
                String prompt = promptBuilder.buildForScenario(scopedContext, scenario, scopedPageSpecs, baselineSpec);
                String fileStem = sanitizeFileStem(scenario.id());
                promptTraceRecorder.recordPrompt(
                        state,
                        "ui-test-spec",
                        fileStem,
                        "ui-test-spec",
                        "scenario:" + scenario.id(),
                        scenario.id(),
                        prompt,
                        buildPromptMetadata(scenario, scopedPageSpecs, baselineSpec, scopedContext)
                );
            }
            state.addArtifact("openai.ui.test.status", "llm-disabled-enrichment-only");
            state.addArtifact("openai.ui.test.scoped.requests", String.valueOf(state.getUiTestPlan().scenarios().size()));
            state.addFinding("OpenAI UI test generation is disabled; prompts were recorded for review only");
            return List.of();
        } catch (Exception exception) {
            if (exception instanceof OpenAiEmptyOutputException emptyOutputException) {
                trackTextArtifact(state, "ui-test-spec", "raw-response-error.json", emptyOutputException.rawResponse());
            }
            trackTextArtifact(state, "ui-test-spec", "error.txt", exception.getMessage());
            state.addArtifact(
                    "openai.ui.test.status",
                    runtimeConfig.strict() ? "active-generation-failed-strict" : "active-generation-failed-no-output"
            );
            state.addFinding("OpenAI UI test generation failed: " + exception.getMessage());
            if (runtimeConfig.strict()) {
                throw new IllegalStateException("OpenAI strict mode rejected UI test spec generation", exception);
            }
            return List.of();
        }
    }

    private List<AiUiTestSpec> filterToPlanScenarios(List<AiUiTestSpec> specs, WorkflowState state) {
        if (specs == null || specs.isEmpty() || state.getUiTestPlan() == null) {
            return List.of();
        }
        Set<String> scenarioIds = state.getUiTestPlan().scenarios().stream().map(scenario -> scenario.id()).collect(Collectors.toSet());
        return specs.stream()
                .filter(spec -> scenarioIds.contains(spec.scenarioId()))
                .toList();
    }

    private List<AiUiTestSpec> filterToScenario(List<AiUiTestSpec> specs, String scenarioId) {
        if (specs == null || specs.isEmpty()) {
            return List.of();
        }
        return specs.stream()
                .filter(spec -> scenarioId.equals(spec.scenarioId()))
                .toList();
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

    private void trackTextArtifact(WorkflowState state, String stage, String fileName, String content) {
        if (state == null) {
            return;
        }
        state.addAiArtifactFile(artifactWriter.writeText(stage, fileName, content).toString());
    }

    private void trackJsonArtifact(WorkflowState state, String stage, String fileName, Object payload) {
        if (state == null) {
            return;
        }
        state.addAiArtifactFile(artifactWriter.writeJson(stage, fileName, payload).toString());
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
