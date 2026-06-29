package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.context.AiContextAssembler;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.openai.OpenAiRuntimeConfig;
import ua.demo.agentlab.ai.quality.AiRunQualityArtifactResult;
import ua.demo.agentlab.ai.quality.AiRunQualitySummaryInput;
import ua.demo.agentlab.ai.quality.AiRunQualitySummaryWriter;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.prompt.quality.PromptQualityGateException;
import ua.demo.agentlab.ai.ui.prompt.quality.PromptQualityReport;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.WorkflowPipelineSnapshot;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiPageObjectSpecGenerator {

    private final OpenAiRuntimeConfig runtimeConfig;
    private final AiContextAssembler contextAssembler;
    private final AiPageObjectScopeResolverStage scopeResolverStage;
    private final AiPageObjectPromptBuildStage promptBuildStage;
    private final AiPageObjectPromptLintStage promptLintStage;
    private final AiPageObjectPromptArtifactWriter promptArtifactWriter;
    private final AiRunQualitySummaryWriter qualitySummaryWriter;

    public AiPageObjectSpecGenerator(OpenAiRuntimeConfig runtimeConfig, AiContextAssembler contextAssembler) {
        this(
                runtimeConfig,
                contextAssembler,
                new AiPageObjectScopeResolverStage(),
                new AiPageObjectPromptBuildStage(),
                new AiPageObjectPromptLintStage(),
                new AiPageObjectPromptArtifactWriter(),
                new AiRunQualitySummaryWriter()
        );
    }

    AiPageObjectSpecGenerator(
            OpenAiRuntimeConfig runtimeConfig,
            AiContextAssembler contextAssembler,
            AiPageObjectScopeResolverStage scopeResolverStage,
            AiPageObjectPromptBuildStage promptBuildStage,
            AiPageObjectPromptLintStage promptLintStage,
            AiPageObjectPromptArtifactWriter promptArtifactWriter,
            AiRunQualitySummaryWriter qualitySummaryWriter
    ) {
        if (runtimeConfig == null || contextAssembler == null) {
            throw new IllegalArgumentException("runtime config and context assembler cannot be null");
        }
        if (scopeResolverStage == null || promptBuildStage == null || promptLintStage == null
                || promptArtifactWriter == null || qualitySummaryWriter == null) {
            throw new IllegalArgumentException("page object generation stages cannot be null");
        }
        this.runtimeConfig = runtimeConfig;
        this.contextAssembler = contextAssembler;
        this.scopeResolverStage = scopeResolverStage;
        this.promptBuildStage = promptBuildStage;
        this.promptLintStage = promptLintStage;
        this.promptArtifactWriter = promptArtifactWriter;
        this.qualitySummaryWriter = qualitySummaryWriter;
    }

    public List<AiPageObjectSpec> generate(WorkflowState state, List<AiPageObjectSpec> baselineSpecs) {
        if (state == null || state.getUiTestPlan() == null) {
            return List.of();
        }
        AiContextPackage contextPackage = state.getAiContextPackage();
        if (contextPackage == null) {
            contextPackage = contextAssembler.assemble(state);
            state.setAiContextPackage(contextPackage);
        }
        AiPageObjectGenerationResult result = generate(new AiPageObjectGenerationRequest(
                WorkflowRunEnvelope.from(state),
                contextPackage,
                state.getUiTestPlan(),
                baselineSpecs,
                new AiRunQualitySummaryInput(
                        state.getKnowledgeRunMetadata(),
                        state.getNormalizedRequirementBundle(),
                        state.getCanonicalTestCaseBundle(),
                        state.getMappedUiKnowledge(),
                        state.getArtifacts()
                ),
                WorkflowPipelineSnapshot.from(state),
                state.getArtifacts()
        ));
        result.artifactFiles().forEach(state::addAiArtifactFile);
        result.artifacts().forEach(state::addArtifact);
        result.findings().forEach(state::addFinding);
        return result.specs();
    }

    public AiPageObjectGenerationResult generate(AiPageObjectGenerationRequest request) {
        if (request == null || request.uiTestPlan() == null || request.contextPackage() == null) {
            return AiPageObjectGenerationResult.empty();
        }

        List<AiPageObjectSpec> specs = new ArrayList<>();
        List<String> artifactFiles = new ArrayList<>();
        Map<String, String> artifacts = new LinkedHashMap<>();
        List<String> findings = new ArrayList<>();

        try {
            for (AiPageObjectPromptScope scope : scopeResolverStage.resolve(request)) {
                findings.addAll(promptLintStage.scopeFindings(scope));
                AiPageObjectPromptDraft draft = promptBuildStage.build(scope);
                PromptQualityReport qualityReport = promptLintStage.validate(draft);
                AiPageObjectPromptArtifactResult artifactResult = promptArtifactWriter.write(draft, qualityReport);
                artifactFiles.addAll(artifactResult.artifactFiles());
                artifacts.putAll(artifactResult.artifacts());
                if (qualityReport.hasBlockingIssues()) {
                    throw new PromptQualityGateException(qualityReport);
                }
            }
            artifacts.put("openai.page.object.status", "llm-disabled-enrichment-only");
            artifacts.put("openai.page.object.scoped.requests", String.valueOf(request.uiTestPlan().pageNames().size()));
            artifacts.put("ai.workflow.terminal.stage", "deterministic-page-object-prompts");
            findings.add("OpenAI page object generation is disabled; prompts were recorded for review only");
            addQualityArtifacts(request, artifacts, artifactFiles);
            return new AiPageObjectGenerationResult(specs, artifactFiles, artifacts, findings);
        } catch (PromptQualityGateException exception) {
            artifactFiles.add(promptArtifactWriter.writeJson(
                    "prompt-quality-blocking-report.json",
                    exception.report()
            ));
            artifacts.put("openai.page.object.status", "prompt-quality-gate-failed");
            findings.add(exception.getMessage());
            addQualityArtifacts(request, artifacts, artifactFiles);
            throw exception;
        } catch (Exception exception) {
            artifactFiles.add(promptArtifactWriter.writeText("error.txt", exception.getMessage()));
            artifacts.put(
                    "openai.page.object.status",
                    runtimeConfig.strict() ? "active-generation-failed-strict" : "active-generation-failed-no-output"
            );
            findings.add("OpenAI page object generation failed: " + exception.getMessage());
            addQualityArtifacts(request, artifacts, artifactFiles);
            if (runtimeConfig.strict()) {
                throw new IllegalStateException("OpenAI strict mode rejected page object spec generation", exception);
            }
            return new AiPageObjectGenerationResult(specs, artifactFiles, artifacts, findings);
        }
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
}
