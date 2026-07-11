package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.openai.OpenAiRuntimeConfigRagAdapter;
import ua.demo.agentlab.ai.openai.OpenAiRuntimeConfig;
import ua.demo.agentlab.ai.quality.AiRunQualityArtifactResult;
import ua.demo.agentlab.ai.quality.AiRunQualitySummaryInput;
import ua.demo.agentlab.ai.quality.AiRunQualitySummaryWriter;
import ua.demo.agentlab.ai.rag.openai.OpenAiResponseGenerationClient;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaValidationException;
import ua.demo.agentlab.ai.ui.contract.DeterministicPomJavaWriter;
import ua.demo.agentlab.ai.ui.contract.PomContractEvidenceRehydrator;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.parser.PomContractSpecParser;
import ua.demo.agentlab.ai.ui.prompt.scope.PomScopeSanitizer;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ai.ui.prompt.quality.PromptQualityGateException;
import ua.demo.agentlab.ai.ui.prompt.quality.PromptQualityReport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiPageObjectSpecGenerator {

    private final OpenAiRuntimeConfig runtimeConfig;
    private final AiPageObjectScopeResolverStage scopeResolverStage;
    private final AiPageObjectPromptBuildStage promptBuildStage;
    private final AiPageObjectPromptLintStage promptLintStage;
    private final AiPageObjectPromptArtifactWriter promptArtifactWriter;
    private final AiRunQualitySummaryWriter qualitySummaryWriter;
    private final PromptPageEligibilityEvaluator promptPageEligibilityEvaluator;
    private final OpenAiResponseGenerationClient generationClient;
    private final PomContractSpecParser contractParser;
    private final PomContractEvidenceRehydrator contractEvidenceRehydrator;
    private final PomScopeSanitizer pomScopeSanitizer;
    private final DeterministicPomJavaWriter compatibilityContractWriter;

    public AiPageObjectSpecGenerator(OpenAiRuntimeConfig runtimeConfig) {
        this(runtimeConfig, "pages");
    }

    public AiPageObjectSpecGenerator(OpenAiRuntimeConfig runtimeConfig, String generatedPagesPackage) {
        this(
                runtimeConfig,
                new AiPageObjectScopeResolverStage(),
                new AiPageObjectPromptBuildStage(),
                new AiPageObjectPromptLintStage(),
                new AiPageObjectPromptArtifactWriter(),
                new AiRunQualitySummaryWriter(),
                new PromptPageEligibilityEvaluator(),
                new OpenAiResponseGenerationClient(new OpenAiRuntimeConfigRagAdapter(runtimeConfig)),
                new PomContractSpecParser(),
                new PomContractEvidenceRehydrator(),
                new PomScopeSanitizer(),
                new DeterministicPomJavaWriter(generatedPagesPackage)
        );
    }

    AiPageObjectSpecGenerator(
            OpenAiRuntimeConfig runtimeConfig,
            AiPageObjectScopeResolverStage scopeResolverStage,
            AiPageObjectPromptBuildStage promptBuildStage,
            AiPageObjectPromptLintStage promptLintStage,
            AiPageObjectPromptArtifactWriter promptArtifactWriter,
            AiRunQualitySummaryWriter qualitySummaryWriter,
            PromptPageEligibilityEvaluator promptPageEligibilityEvaluator,
            OpenAiResponseGenerationClient generationClient,
            PomContractSpecParser contractParser,
            PomContractEvidenceRehydrator contractEvidenceRehydrator,
            PomScopeSanitizer pomScopeSanitizer,
            DeterministicPomJavaWriter compatibilityContractWriter
    ) {
        if (runtimeConfig == null) {
            throw new IllegalArgumentException("runtime config cannot be null");
        }
        if (scopeResolverStage == null || promptBuildStage == null || promptLintStage == null
                || promptArtifactWriter == null || qualitySummaryWriter == null || promptPageEligibilityEvaluator == null
                || generationClient == null || contractParser == null || contractEvidenceRehydrator == null
                || pomScopeSanitizer == null || compatibilityContractWriter == null) {
            throw new IllegalArgumentException("page object generation stages cannot be null");
        }
        this.runtimeConfig = runtimeConfig;
        this.scopeResolverStage = scopeResolverStage;
        this.promptBuildStage = promptBuildStage;
        this.promptLintStage = promptLintStage;
        this.promptArtifactWriter = promptArtifactWriter;
        this.qualitySummaryWriter = qualitySummaryWriter;
        this.promptPageEligibilityEvaluator = promptPageEligibilityEvaluator;
        this.generationClient = generationClient;
        this.contractParser = contractParser;
        this.contractEvidenceRehydrator = contractEvidenceRehydrator;
        this.pomScopeSanitizer = pomScopeSanitizer;
        this.compatibilityContractWriter = compatibilityContractWriter;
    }

    public AiPageObjectGenerationResult generate(AiPageObjectGenerationRequest request) {
        if (request == null || request.uiTestPlan() == null || request.contextPackage() == null) {
            return AiPageObjectGenerationResult.empty();
        }

        List<AiPageObjectSpec> specs = new ArrayList<>();
        List<PomContractSpec> contracts = new ArrayList<>();
        List<String> artifactFiles = new ArrayList<>();
        Map<String, String> artifacts = new LinkedHashMap<>();
        List<String> findings = new ArrayList<>();
        boolean llmRequested = runtimeConfig.pageObjectLlmEnabled();
        boolean llmEnabled = llmRequested && runtimeConfig.enabled() && hasApiKey();
        int pomLlmAttempts = 0;
        int pomLlmSuccesses = 0;
        int pomLlmPromptChars = 0;
        int pomLlmResponseChars = 0;
        int pomLlmInputTokens = 0;
        int pomLlmOutputTokens = 0;
        int pomLlmTotalTokens = 0;
        addRetrievalHealthArtifacts(request.contextPackage(), artifacts);

        try {
            if (llmRequested && !llmEnabled) {
                artifacts.put("ai.page-object.llm.requested", "true");
                artifacts.put("ai.page-object.llm.skipped", "true");
                artifacts.put("ai.page-object.llm.skipReason", "missing-openai-api-key-or-openai-disabled");
                findings.add("POM contract LLM was requested but skipped because OpenAI API key/config is unavailable; prompts were recorded only");
            }
            for (AiPageObjectPromptScope scope : scopeResolverStage.resolve(request)) {
                findings.addAll(promptLintStage.scopeFindings(scope));
                PromptPage promptPage = promptPageEligibilityEvaluator.evaluate(scope);
                artifacts.put(
                        "ai.page.object.prompt." + scope.fileStem() + ".eligible",
                        String.valueOf(promptPage.eligible())
                );
                if (!promptPage.eligible()) {
                    artifactFiles.add(promptArtifactWriter.writeJson(
                            scope.fileStem() + "-prompt-page-eligibility.json",
                            promptPage
                    ));
                    artifacts.put(
                            "ai.page.object.prompt." + scope.fileStem() + ".skipped",
                            "true"
                    );
                    artifacts.put(
                            "ai.page.object.prompt." + scope.fileStem() + ".skipReason",
                            String.join("; ", promptPage.reasons())
                    );
                    findings.add("Skipped page object prompt for " + scope.pageName() + ": "
                            + String.join("; ", promptPage.reasons()));
                    continue;
                }
                AiPageObjectPromptDraft draft = promptBuildStage.build(scope);
                PromptQualityReport qualityReport = promptLintStage.validate(draft);
                AiPageObjectPromptArtifactResult artifactResult = promptArtifactWriter.write(draft, qualityReport);
                artifactFiles.addAll(artifactResult.artifactFiles());
                artifacts.putAll(artifactResult.artifacts());
                if (qualityReport.hasBlockingIssues()) {
                    throw new PromptQualityGateException(qualityReport);
                }
                if (llmEnabled) {
                    pomLlmAttempts++;
                    pomLlmPromptChars += draft.prompt().length();
                    String response = generationClient.generate(draft.prompt());
                    pomLlmResponseChars += response == null ? 0 : response.length();
                    var usage = generationClient.lastUsage();
                    pomLlmInputTokens += usage.inputTokens();
                    pomLlmOutputTokens += usage.outputTokens();
                    pomLlmTotalTokens += usage.totalTokens();
                    promptArtifactWriter.writeDebugText(scope.fileStem() + "-pom-contract-response.txt", response)
                            .ifPresent(artifactFiles::add);
                    PromptReadyPomScope readyScope = pomScopeSanitizer.sanitize(
                            scope.scopedContext(),
                            scope.pageName(),
                            scope.pageScenarios()
                    );
                    PomContractSpec contract = contractEvidenceRehydrator.rehydrate(
                            contractParser.parse(response),
                            readyScope,
                            scope.scopedContext()
                    );
                    contracts.add(contract);
                    pomLlmSuccesses++;
                    specs.add(compatibilityContractWriter.toAiPageObjectSpec(contract));
                    artifactFiles.add(promptArtifactWriter.writeJson(scope.fileStem() + "-pom-contract.json", contract));
                    artifacts.put("pom.contract." + scope.fileStem() + ".pageName", contract.page().name());
                    artifacts.put("pom.contract." + scope.fileStem() + ".locator.count",
                            String.valueOf(contract.locators().size()));
                    artifacts.put("pom.contract." + scope.fileStem() + ".action.count",
                            String.valueOf(contract.actions().size()));
                    artifacts.put("pom.contract." + scope.fileStem() + ".assertion.count",
                            String.valueOf(contract.assertions().size()));
                }
            }
            artifacts.put("openai.page.object.status", llmEnabled
                    ? "pom-contract-llm-generated"
                    : llmRequested ? "pom-contract-llm-skipped-prompt-only" : "llm-disabled-enrichment-only");
            putPomLlmArtifacts(artifacts, pomLlmAttempts, pomLlmSuccesses, pomLlmPromptChars, pomLlmResponseChars,
                    pomLlmInputTokens, pomLlmOutputTokens, pomLlmTotalTokens);
            artifacts.put("ai.page-object.llm.requested", String.valueOf(llmRequested));
            artifacts.put("ai.page-object.llm.enabled", String.valueOf(llmEnabled));
            artifacts.put("openai.page.object.scoped.requests", String.valueOf(request.uiTestPlan().pageNames().size()));
            artifacts.put("pom.contract.spec.count", String.valueOf(contracts.size()));
            artifacts.put("ai.workflow.terminal.stage", llmEnabled
                    ? "pom-contract-deterministic-java"
                    : "deterministic-page-object-prompts");
            artifactFiles.add(promptArtifactWriter.writeJson("pom-llm-token-usage.json", Map.of(
                    "schemaVersion", "llm-token-usage.v1",
                    "stage", "pom-contract",
                    "attempts", pomLlmAttempts,
                    "successes", pomLlmSuccesses,
                    "promptChars", pomLlmPromptChars,
                    "responseChars", pomLlmResponseChars,
                    "inputTokens", pomLlmInputTokens,
                    "outputTokens", pomLlmOutputTokens,
                    "totalTokens", pomLlmTotalTokens
            )));
            findings.add(llmEnabled
                    ? "OpenAI generated POM contract JSON; Java will be written by deterministic writer"
                    : "OpenAI page object generation is disabled; prompts were recorded for review only");
            addQualityArtifacts(request, artifacts, artifactFiles);
            return new AiPageObjectGenerationResult(specs, contracts, artifactFiles, artifacts, findings);
        } catch (PromptQualityGateException exception) {
            artifactFiles.add(promptArtifactWriter.writeJson(
                    "prompt-quality-blocking-report.json",
                    exception.report()
            ));
            artifacts.put("openai.page.object.status", "prompt-quality-gate-failed");
            putPomLlmArtifacts(artifacts, pomLlmAttempts, pomLlmSuccesses, pomLlmPromptChars, pomLlmResponseChars,
                    pomLlmInputTokens, pomLlmOutputTokens, pomLlmTotalTokens);
            findings.add(exception.getMessage());
            addQualityArtifacts(request, artifacts, artifactFiles);
            throw exception;
        } catch (Exception exception) {
            if (exception instanceof LlmOutputSchemaValidationException validationException) {
                artifactFiles.add(promptArtifactWriter.writeJson(
                        "pom-contract-validation-report.json",
                        validationException.report()
                ));
            }
            artifactFiles.add(promptArtifactWriter.writeText("error.txt", exception.getMessage()));
            artifacts.put(
                    "openai.page.object.status",
                    runtimeConfig.strict() ? "active-generation-failed-strict" : "active-generation-failed-no-output"
            );
            putPomLlmArtifacts(artifacts, pomLlmAttempts, pomLlmSuccesses, pomLlmPromptChars, pomLlmResponseChars,
                    pomLlmInputTokens, pomLlmOutputTokens, pomLlmTotalTokens);
            findings.add("OpenAI page object generation failed: " + exception.getMessage());
            addQualityArtifacts(request, artifacts, artifactFiles);
            if (runtimeConfig.strict()) {
                throw new IllegalStateException("OpenAI strict mode rejected page object spec generation", exception);
            }
            return new AiPageObjectGenerationResult(specs, contracts, artifactFiles, artifacts, findings);
        }
    }

    private void putPomLlmArtifacts(
            Map<String, String> artifacts,
            int attempts,
            int successes,
            int promptChars,
            int responseChars,
            int inputTokens,
            int outputTokens,
            int totalTokens
    ) {
        int failures = Math.max(0, attempts - successes);
        artifacts.put("pom.contract.llm.attempt.count", String.valueOf(Math.max(0, attempts)));
        artifacts.put("pom.contract.llm.success.count", String.valueOf(Math.max(0, successes)));
        artifacts.put("pom.contract.llm.failure.count", String.valueOf(failures));
        artifacts.put("pom.contract.llm.prompt.chars", String.valueOf(Math.max(0, promptChars)));
        artifacts.put("pom.contract.llm.response.chars", String.valueOf(Math.max(0, responseChars)));
        artifacts.put("pom.contract.llm.input.tokens", String.valueOf(Math.max(0, inputTokens)));
        artifacts.put("pom.contract.llm.output.tokens", String.valueOf(Math.max(0, outputTokens)));
        artifacts.put("pom.contract.llm.total.tokens", String.valueOf(Math.max(0, totalTokens)));
    }

    private void addQualityArtifacts(
            AiPageObjectGenerationRequest request,
            Map<String, String> artifacts,
            List<String> artifactFiles
    ) {
        AiRunQualityArtifactResult qualityResult = qualitySummaryWriter.write(
                mergeQualityArtifacts(request.qualitySummaryInput(), request.artifacts(), artifacts),
                request.pipelineSnapshot()
        );
        artifactFiles.addAll(qualityResult.artifactFiles());
        artifacts.putAll(qualityResult.artifacts());
    }

    private void addRetrievalHealthArtifacts(AiContextPackage context, Map<String, String> artifacts) {
        if (context == null || context.retrievalContext() == null || artifacts == null) {
            return;
        }
        var retrieval = context.retrievalContext();
        artifacts.put("ui.knowledge.retrieval.neo4j.hit", String.valueOf(retrieval.neo4jHit()));
        artifacts.put("ui.knowledge.retrieval.qdrant.hit", String.valueOf(retrieval.qdrantHit()));
        artifacts.put("ui.knowledge.retrieval.mode", retrieval.retrievalMode());
        artifacts.put("ui.knowledge.retrieval.stable.cache.used", String.valueOf(retrieval.stableCacheUsed()));
        artifacts.put("ui.knowledge.retrieval.stale.evidence.rejected", String.valueOf(retrieval.staleEvidenceRejected()));
        artifacts.put("ui.knowledge.retrieval.vector.unavailable.reason", retrieval.vectorUnavailableReason());
    }

    private AiRunQualitySummaryInput mergeQualityArtifacts(
            AiRunQualitySummaryInput input,
            Map<String, String> originalArtifacts,
            Map<String, String> generatedArtifacts
    ) {
        Map<String, String> mergedArtifacts = new LinkedHashMap<>();
        if (originalArtifacts != null) {
            mergedArtifacts.putAll(originalArtifacts);
        }
        if (input != null) {
            mergedArtifacts.putAll(input.artifacts());
        }
        if (generatedArtifacts != null) {
            mergedArtifacts.putAll(generatedArtifacts);
        }
        return new AiRunQualitySummaryInput(
                input == null ? null : input.knowledgeRunMetadata(),
                input == null ? null : input.normalizedRequirementBundle(),
                input == null ? null : input.canonicalTestCaseBundle(),
                input == null ? null : input.mappedUiKnowledge(),
                mergedArtifacts
        );
    }

    private boolean hasApiKey() {
        String apiKey = runtimeConfig.apiKey();
        return apiKey != null && !apiKey.isBlank();
    }
}
