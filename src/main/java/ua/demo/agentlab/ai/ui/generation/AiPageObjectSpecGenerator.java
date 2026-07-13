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
import ua.demo.agentlab.ai.ui.contract.PomContractQualityReport;
import ua.demo.agentlab.ai.ui.contract.PomContractScopeGapReconciler;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractScopeValidator;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.parser.PomContractSpecParser;
import ua.demo.agentlab.ai.ui.prompt.scope.PomScopeSanitizer;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ai.ui.prompt.quality.PromptQualityGateException;
import ua.demo.agentlab.ai.ui.prompt.quality.PromptQualityReport;
import ua.demo.agentlab.artifactreuse.config.ArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.config.PropertiesArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.fingerprint.ArtifactFingerprint;
import ua.demo.agentlab.artifactreuse.fingerprint.PomContractFingerprintBuilder;
import ua.demo.agentlab.artifactreuse.fingerprint.PomContractFingerprintInput;
import ua.demo.agentlab.artifactreuse.model.ArtifactRecord;
import ua.demo.agentlab.artifactreuse.model.ArtifactRunRelation;
import ua.demo.agentlab.artifactreuse.model.ArtifactReuseDecisionTrace;
import ua.demo.agentlab.artifactreuse.model.ArtifactStatus;
import ua.demo.agentlab.artifactreuse.model.ArtifactTarget;
import ua.demo.agentlab.artifactreuse.model.ArtifactTargetType;
import ua.demo.agentlab.artifactreuse.model.ArtifactType;
import ua.demo.agentlab.artifactreuse.model.PomContractArtifactProvenance;
import ua.demo.agentlab.artifactreuse.model.QualityGateRecord;
import ua.demo.agentlab.artifactreuse.model.RunRecord;
import ua.demo.agentlab.artifactreuse.policy.ArtifactReuseDecision;
import ua.demo.agentlab.artifactreuse.policy.ArtifactReusePolicy;
import ua.demo.agentlab.artifactreuse.policy.ArtifactReusePolicyInput;
import ua.demo.agentlab.artifactreuse.registry.ArtifactLookupRequest;
import ua.demo.agentlab.artifactreuse.registry.ArtifactLookupResult;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistry;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistryWriteRequest;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistryWriteResult;
import ua.demo.agentlab.artifactreuse.registry.neo4j.Neo4jArtifactRegistry;
import ua.demo.agentlab.artifactreuse.store.FileBackedStableArtifactStore;
import ua.demo.agentlab.artifactreuse.store.StableArtifactLookup;
import ua.demo.agentlab.artifactreuse.store.StableArtifactWriteResult;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;

import java.nio.file.Path;
import java.time.Instant;
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
    private final PomContractScopeGapReconciler scopeGapReconciler = new PomContractScopeGapReconciler();
    private final PomContractScopeValidator contractScopeValidator = new PomContractScopeValidator();
    private final DeterministicPomJavaWriter compatibilityContractWriter;
    private final ArtifactReuseRuntimeConfig artifactReuseConfig;
    private final PomContractFingerprintBuilder fingerprintBuilder;
    private final FileBackedStableArtifactStore stableArtifactStore;
    private final ArtifactReusePolicy artifactReusePolicy;
    private final ArtifactRegistry artifactRegistry;

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
                new DeterministicPomJavaWriter(generatedPagesPackage),
                new PropertiesArtifactReuseRuntimeConfig(),
                new PomContractFingerprintBuilder(),
                new FileBackedStableArtifactStore(new PropertiesArtifactReuseRuntimeConfig().stableRoot()),
                new ArtifactReusePolicy(),
                new Neo4jArtifactRegistry(new PropertiesNeo4jRuntimeConfig())
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
            DeterministicPomJavaWriter compatibilityContractWriter,
            ArtifactReuseRuntimeConfig artifactReuseConfig,
            PomContractFingerprintBuilder fingerprintBuilder,
            FileBackedStableArtifactStore stableArtifactStore,
            ArtifactReusePolicy artifactReusePolicy,
            ArtifactRegistry artifactRegistry
    ) {
        if (runtimeConfig == null) {
            throw new IllegalArgumentException("runtime config cannot be null");
        }
        if (scopeResolverStage == null || promptBuildStage == null || promptLintStage == null
                || promptArtifactWriter == null || qualitySummaryWriter == null || promptPageEligibilityEvaluator == null
                || generationClient == null || contractParser == null || contractEvidenceRehydrator == null
                || pomScopeSanitizer == null || compatibilityContractWriter == null
                || artifactReuseConfig == null || fingerprintBuilder == null || stableArtifactStore == null
                || artifactReusePolicy == null || artifactRegistry == null) {
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
        this.artifactReuseConfig = artifactReuseConfig;
        this.fingerprintBuilder = fingerprintBuilder;
        this.stableArtifactStore = stableArtifactStore;
        this.artifactReusePolicy = artifactReusePolicy;
        this.artifactRegistry = artifactRegistry;
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
        int artifactReuseHits = 0;
        int artifactReuseMisses = 0;
        int artifactReuseSkippedLlmCalls = 0;
        int artifactReuseTokensSavedEstimate = 0;
        addRetrievalHealthArtifacts(request.contextPackage(), artifacts);
        artifacts.put("artifact.reuse.enabled", String.valueOf(artifactReuseConfig.enabled()));
        artifacts.put("artifact.reuse.forceRefresh", String.valueOf(artifactReuseConfig.forceRefresh()));

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
                ArtifactFingerprint fingerprint = fingerprint(scope, draft, request);
                artifacts.put("artifact.reuse." + scope.fileStem() + ".fingerprint", fingerprint.value());
                ArtifactLookupResult registryLookup = artifactRegistry.findStableArtifact(new ArtifactLookupRequest(
                        ArtifactType.POM_CONTRACT,
                        targetId(scope),
                        fingerprint.value(),
                        String.valueOf(draft.metadata().getOrDefault("schemaVersion", ""))
                ));
                StableArtifactLookup stableLookup = stableLookup(registryLookup, scope, fingerprint);
                ArtifactReuseDecision reuseDecision = artifactReusePolicy.decide(new ArtifactReusePolicyInput(
                        artifactReuseConfig.enabled() && artifactReuseConfig.pomContractEnabled(),
                        artifactReuseConfig.forceRefresh(),
                        registryLookup.hit() || stableLookup.hit(),
                        stableLookup.hit(),
                        registryLookup.artifact() == null
                                ? (stableLookup.hit() ? "STABLE" : "")
                                : registryLookup.artifact().status().name(),
                        String.valueOf(draft.metadata().getOrDefault("schemaVersion", "")),
                        registryLookup.artifact() == null ? "" : registryLookup.artifact().schemaVersion()
                ));
                putArtifactReuseDecisionArtifacts(artifacts, scope, registryLookup, stableLookup, reuseDecision);
                artifactFiles.add(promptArtifactWriter.writeJson(scope.fileStem() + "-artifact-reuse-decision.json",
                        new ArtifactReuseDecisionTrace(
                                scope.pageName(), targetId(scope), fingerprint.value(), registryLookup.attempted(), registryLookup.hit(),
                                registryLookup.message(), stableLookup.hit(),
                                stableLookup.path() == null ? "" : stableLookup.path().toString(),
                                reuseDecision.type().name(), reuseDecision.reason()
                        )));
                if (reuseDecision.reuseStable()) {
                    PromptReadyPomScope readyScope = pomScopeSanitizer.sanitize(
                            scope.scopedContext(), scope.pageName(), scope.pageScenarios());
                    PomContractSpec contract = reconcileScopeCoverageGaps(contractEvidenceRehydrator.rehydrate(
                            stableLookup.pomContract(), readyScope, scope.scopedContext()), readyScope);
                    validateContractScope(contract, readyScope, scope, artifactFiles);
                    contracts.add(contract);
                    specs.add(compatibilityContractWriter.toAiPageObjectSpec(contract));
                    artifactFiles.add(stableLookup.path().toString());
                    artifactFiles.add(promptArtifactWriter.writeJson(scope.fileStem() + "-pom-contract.json", contract));
                    artifactFiles.add(promptArtifactWriter.writeJson(
                            scope.fileStem() + "-pom-contract-provenance.json",
                            provenance(scope, fingerprint, "REUSED_STABLE", stableLookup.path(), registryLookup.artifact(), request)
                    ));
                    artifactReuseHits++;
                    artifactReuseSkippedLlmCalls++;
                    artifactReuseTokensSavedEstimate += estimateTokens(draft.prompt());
                    registerReusedArtifact(scope, fingerprint, contract, stableLookup.path(), request,
                            registryLookup.artifact(), artifacts);
                    putContractArtifacts(artifacts, scope, contract);
                    findings.add("Reused stable POM contract artifact for " + scope.pageName());
                    continue;
                }
                artifactReuseMisses++;
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
                    PomContractSpec contract = reconcileScopeCoverageGaps(contractEvidenceRehydrator.rehydrate(
                            contractParser.parse(response),
                            readyScope,
                            scope.scopedContext()
                    ), readyScope);
                    validateContractScope(contract, readyScope, scope, artifactFiles);
                    contracts.add(contract);
                    pomLlmSuccesses++;
                    specs.add(compatibilityContractWriter.toAiPageObjectSpec(contract));
                    artifactFiles.add(promptArtifactWriter.writeJson(scope.fileStem() + "-pom-contract.json", contract));
                    StableArtifactWriteResult writeResult = stableArtifactStore.writePomContract(
                            scope.pageName(),
                            fingerprint.value(),
                            contract
                    );
                    if (writeResult.success() && writeResult.path() != null) {
                        artifactFiles.add(writeResult.path().toString());
                    }
                    artifactFiles.add(promptArtifactWriter.writeJson(
                            scope.fileStem() + "-pom-contract-provenance.json",
                            provenance(scope, fingerprint, "GENERATED_CURRENT_RUN", writeResult.path(), null, request)
                    ));
                    artifacts.put("artifact.reuse." + scope.fileStem() + ".stableWrite.success",
                            String.valueOf(writeResult.success()));
                    artifacts.put("artifact.reuse." + scope.fileStem() + ".stableWrite.path",
                            writeResult.path() == null ? "" : writeResult.path().toString());
                    registerArtifact(scope, fingerprint, contract, writeResult.path(), request, ArtifactRunRelation.PRODUCED,
                            0, artifacts);
                    putContractArtifacts(artifacts, scope, contract);
                }
            }
            artifacts.put("openai.page.object.status", llmEnabled
                    ? "pom-contract-llm-generated"
                    : llmRequested ? "pom-contract-llm-skipped-prompt-only" : "llm-disabled-enrichment-only");
            putPomLlmArtifacts(artifacts, pomLlmAttempts, pomLlmSuccesses, pomLlmPromptChars, pomLlmResponseChars,
                    pomLlmInputTokens, pomLlmOutputTokens, pomLlmTotalTokens);
            putArtifactReuseArtifacts(artifacts, artifactReuseHits, artifactReuseMisses, artifactReuseSkippedLlmCalls,
                    artifactReuseTokensSavedEstimate);
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
            putArtifactReuseArtifacts(artifacts, artifactReuseHits, artifactReuseMisses, artifactReuseSkippedLlmCalls,
                    artifactReuseTokensSavedEstimate);
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
            putArtifactReuseArtifacts(artifacts, artifactReuseHits, artifactReuseMisses, artifactReuseSkippedLlmCalls,
                    artifactReuseTokensSavedEstimate);
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

    private void validateContractScope(
            PomContractSpec contract,
            PromptReadyPomScope readyScope,
            AiPageObjectPromptScope scope,
            List<String> artifactFiles
    ) {
        PomContractQualityReport report = contractScopeValidator.validate(contract, readyScope);
        artifactFiles.add(promptArtifactWriter.writeJson(scope.fileStem() + "-contract-scope-quality.json", report));
        if (report.hasBlockingIssues()) {
            throw new IllegalStateException("POM contract scope gate failed for " + scope.pageName()
                    + " with " + report.blockingIssueCount() + " blocking issue(s)");
        }
    }

    private PomContractSpec reconcileScopeCoverageGaps(PomContractSpec contract, PromptReadyPomScope readyScope) {
        return scopeGapReconciler.reconcile(contract, readyScope);
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

    private ArtifactFingerprint fingerprint(
            AiPageObjectPromptScope scope,
            AiPageObjectPromptDraft draft,
            AiPageObjectGenerationRequest request
    ) {
        PromptReadyPomScope promptScope = pomScopeSanitizer.sanitize(
                scope.scopedContext(), scope.pageName(), scope.pageScenarios());
        KnowledgeRunMetadata metadata = request == null || request.qualitySummaryInput() == null
                ? null
                : request.qualitySummaryInput().knowledgeRunMetadata();
        return fingerprintBuilder.build(new PomContractFingerprintInput(
                targetId(scope),
                scope.pageName(),
                targetRoute(scope),
                capability(scope),
                scope.scopedContext() == null ? null : scope.scopedContext().promptUiEvidence(),
                draft.metadata(),
                artifactReuseConfig.promptTemplateVersion(),
                String.valueOf(draft.metadata().getOrDefault("schemaVersion", "")),
                runtimeConfig.model(),
                0.0d,
                runtimeConfig.pageObjectLlmEnabled() ? "llm-pom-contract" : "prompt-only",
                scope.scopedContext() == null || scope.scopedContext().retrievalContext() == null
                        ? ""
                        : scope.scopedContext().retrievalContext().retrievalMode(),
                artifactReuseConfig.writerVersion(),
                promptScope,
                metadata == null ? "" : metadata.appId(),
                metadata == null ? "" : metadata.baseUrlHash()
        ));
    }

    private StableArtifactLookup stableLookup(
            ArtifactLookupResult registryLookup,
            AiPageObjectPromptScope scope,
            ArtifactFingerprint fingerprint
    ) {
        if (registryLookup != null && registryLookup.hit()) {
            String registryPath = registryLookup.artifact() == null ? "" : registryLookup.artifact().filePath();
            if (registryPath != null && !registryPath.isBlank()) {
                StableArtifactLookup byRegistryPath = stableArtifactStore.findPomContract(Path.of(registryPath));
                if (byRegistryPath.hit()) {
                    return byRegistryPath;
                }
            }
        }
        return stableArtifactStore.findValidatedPomContract(scope.pageName(), fingerprint.value());
    }

    private void putArtifactReuseDecisionArtifacts(
            Map<String, String> artifacts,
            AiPageObjectPromptScope scope,
            ArtifactLookupResult registryLookup,
            StableArtifactLookup stableLookup,
            ArtifactReuseDecision decision
    ) {
        String prefix = "artifact.reuse." + scope.fileStem() + ".";
        artifacts.put(prefix + "registry.attempted", String.valueOf(registryLookup.attempted()));
        artifacts.put(prefix + "registry.hit", String.valueOf(registryLookup.hit()));
        artifacts.put(prefix + "registry.message", registryLookup.message());
        artifacts.put(prefix + "file.hit", String.valueOf(stableLookup.hit()));
        artifacts.put(prefix + "file.path", stableLookup.path() == null ? "" : stableLookup.path().toString());
        artifacts.put(prefix + "decision", decision.type().name());
        artifacts.put(prefix + "decision.reason", decision.reason());
    }

    private void putArtifactReuseArtifacts(
            Map<String, String> artifacts,
            int hits,
            int misses,
            int skippedLlmCalls,
            int tokensSavedEstimate
    ) {
        artifacts.put("artifact.reuse.hit.count", String.valueOf(Math.max(0, hits)));
        artifacts.put("artifact.reuse.miss.count", String.valueOf(Math.max(0, misses)));
        artifacts.put("artifact.reuse.llm.skipped.count", String.valueOf(Math.max(0, skippedLlmCalls)));
        artifacts.put("artifact.reuse.tokens.saved.estimate", String.valueOf(Math.max(0, tokensSavedEstimate)));
    }

    private void putContractArtifacts(
            Map<String, String> artifacts,
            AiPageObjectPromptScope scope,
            PomContractSpec contract
    ) {
        artifacts.put("pom.contract." + scope.fileStem() + ".pageName", contract.page().name());
        artifacts.put("pom.contract." + scope.fileStem() + ".locator.count", String.valueOf(contract.locators().size()));
        artifacts.put("pom.contract." + scope.fileStem() + ".action.count", String.valueOf(contract.actions().size()));
        artifacts.put("pom.contract." + scope.fileStem() + ".assertion.count", String.valueOf(contract.assertions().size()));
    }

    private PomContractArtifactProvenance provenance(
            AiPageObjectPromptScope scope,
            ArtifactFingerprint fingerprint,
            String source,
            Path stablePath,
            ArtifactRecord originArtifact,
            AiPageObjectGenerationRequest request
    ) {
        KnowledgeRunMetadata metadata = request == null || request.qualitySummaryInput() == null
                ? null
                : request.qualitySummaryInput().knowledgeRunMetadata();
        return new PomContractArtifactProvenance(
                "pom-contract-artifact-provenance.v1",
                scope.pageName(),
                source,
                fingerprint == null ? "" : fingerprint.value(),
                stablePath == null ? "" : stablePath.toString(),
                originArtifact == null ? "" : originArtifact.artifactId(),
                metadata == null ? "" : metadata.runId()
        );
    }

    private void registerArtifact(
            AiPageObjectPromptScope scope,
            ArtifactFingerprint fingerprint,
            PomContractSpec contract,
            Path filePath,
            AiPageObjectGenerationRequest request,
            ArtifactRunRelation relation,
            long reuseCount,
            Map<String, String> artifacts
    ) {
        ArtifactRecord artifact = new ArtifactRecord(
                artifactId(scope, fingerprint),
                ArtifactType.POM_CONTRACT,
                ArtifactTargetType.PAGE,
                targetId(scope),
                fingerprint.value(),
                contract.schemaVersion(),
                artifactReuseConfig.promptTemplateVersion(),
                runtimeConfig.model(),
                0.0d,
                ArtifactStatus.SCHEMA_VALIDATED,
                0.0d,
                false,
                false,
                filePath == null ? "" : filePath.toString(),
                Instant.now().toString(),
                Instant.now().toString(),
                reuseCount
        );
        ArtifactRegistryWriteResult result = artifactRegistry.register(new ArtifactRegistryWriteRequest(
                artifact,
                ArtifactTarget.page(targetId(scope), scope.pageName(), targetRoute(scope), capability(scope)),
                runRecord(request),
                relation,
                List.of(new QualityGateRecord(
                        artifact.artifactId() + "-schema",
                        "SCHEMA",
                        "PASSED",
                        "pom-contract parsed and rehydrated",
                        0,
                        Instant.now().toString()
                ))
        ));
        String prefix = "artifact.registry." + scope.fileStem() + ".";
        artifacts.put(prefix + "attempted", String.valueOf(result.attempted()));
        artifacts.put(prefix + "success", String.valueOf(result.success()));
        artifacts.put(prefix + "relation", relation.name());
        artifacts.put(prefix + "message", result.message());
    }

    private void registerReusedArtifact(
            AiPageObjectPromptScope scope,
            ArtifactFingerprint fingerprint,
            PomContractSpec contract,
            Path filePath,
            AiPageObjectGenerationRequest request,
            ArtifactRecord existing,
            Map<String, String> artifacts
    ) {
        ArtifactRecord artifact = new ArtifactRecord(
                existing == null || existing.artifactId().isBlank() ? artifactId(scope, fingerprint) : existing.artifactId(),
                ArtifactType.POM_CONTRACT,
                ArtifactTargetType.PAGE,
                targetId(scope),
                fingerprint.value(),
                contract.schemaVersion(),
                artifactReuseConfig.promptTemplateVersion(),
                existing == null || existing.model().isBlank() ? runtimeConfig.model() : existing.model(),
                existing == null ? 0.0d : existing.temperature(),
                ArtifactStatus.STABLE,
                existing == null ? 0.0d : existing.qualityScore(),
                true,
                true,
                filePath == null ? "" : filePath.toString(),
                existing == null ? Instant.now().toString() : existing.createdAt(),
                Instant.now().toString(),
                existing == null ? 1L : existing.reuseCount() + 1L
        );
        ArtifactRegistryWriteResult result = artifactRegistry.register(new ArtifactRegistryWriteRequest(
                artifact,
                ArtifactTarget.page(targetId(scope), scope.pageName(), targetRoute(scope), capability(scope)),
                runRecord(request),
                ArtifactRunRelation.REUSED,
                List.of(new QualityGateRecord(
                        artifact.artifactId() + "-reuse",
                        "REUSE",
                        "PASSED",
                        "stable POM contract reused and queued for current-run validation",
                        0,
                        Instant.now().toString()
                ))
        ));
        String prefix = "artifact.registry." + scope.fileStem() + ".";
        artifacts.put(prefix + "attempted", String.valueOf(result.attempted()));
        artifacts.put(prefix + "success", String.valueOf(result.success()));
        artifacts.put(prefix + "relation", ArtifactRunRelation.REUSED.name());
        artifacts.put(prefix + "message", result.message());
    }

    private RunRecord runRecord(AiPageObjectGenerationRequest request) {
        KnowledgeRunMetadata metadata = request == null || request.qualitySummaryInput() == null
                ? null
                : request.qualitySummaryInput().knowledgeRunMetadata();
        if (metadata != null) {
            return new RunRecord(
                    metadata.runId(),
                    metadata.appId(),
                    metadata.baseUrlHash(),
                    metadata.requirementSetHash(),
                    metadata.discoverySessionId(),
                    metadata.schemaVersion(),
                    metadata.createdAt(),
                    "ai-page-object-spec-generator"
            );
        }
        String runId = request == null || request.runEnvelope() == null || request.runEnvelope().runMetadata() == null
                ? ""
                : request.runEnvelope().runMetadata().runId();
        String createdAt = request == null || request.runEnvelope() == null || request.runEnvelope().runMetadata() == null
                ? Instant.now().toString()
                : request.runEnvelope().runMetadata().createdAt().toString();
        return new RunRecord(runId, "", "", "", "", "", createdAt, "ai-page-object-spec-generator");
    }

    private String artifactId(AiPageObjectPromptScope scope, ArtifactFingerprint fingerprint) {
        return "pom-contract:" + targetId(scope) + ":" + fingerprint.value();
    }

    private String targetId(AiPageObjectPromptScope scope) {
        if (scope == null) {
            return "";
        }
        if (scope.scopedContext() != null && scope.scopedContext().promptUiEvidence() != null
                && !scope.scopedContext().promptUiEvidence().targetPage().isBlank()) {
            return scope.scopedContext().promptUiEvidence().targetPage();
        }
        return scope.pageName();
    }

    private String targetRoute(AiPageObjectPromptScope scope) {
        if (scope == null) {
            return "";
        }
        if (scope.scopedContext() != null && scope.scopedContext().promptUiEvidence() != null
                && !scope.scopedContext().promptUiEvidence().targetRoute().isBlank()) {
            return scope.scopedContext().promptUiEvidence().targetRoute();
        }
        if (scope.pageScope() != null && !scope.pageScope().targetRoutes().isEmpty()) {
            return scope.pageScope().targetRoutes().get(0);
        }
        return "";
    }

    private String capability(AiPageObjectPromptScope scope) {
        if (scope == null || scope.scopedContext() == null || scope.scopedContext().promptUiEvidence() == null) {
            return "";
        }
        String evidence = (scope.scopedContext().promptUiEvidence().requiredActions() + " "
                + scope.scopedContext().promptUiEvidence().requiredAssertions()).toLowerCase(java.util.Locale.ROOT);
        if (evidence.contains("authenticate") || evidence.contains("login")) {
            return "AUTHENTICATION";
        }
        if (evidence.contains("logout") || evidence.contains("authenticated")) {
            return "AUTHENTICATED_AREA";
        }
        return "";
    }

    private int estimateTokens(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(prompt.length() / 4.0d));
    }
}
