package ua.demo.agentlab.ai.ui.agent;

import ua.demo.agentlab.ai.quality.AiRunQualitySummaryInput;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectGenerationRequest;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectGenerationResult;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectSpecGenerator;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowPipelineSnapshot;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelReport;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class AiPageObjectSpecAgent implements WorkflowAgent,
        PipelineAgent<AiPageObjectSpecInput, AiPageObjectGenerationResult> {

    private final AiPageObjectSpecGenerator generator;
    private final Function<WorkflowState, List<AiPageObjectSpec>> baselineProvider;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public AiPageObjectSpecAgent(AiPageObjectSpecGenerator generator) {
        this(generator, state -> List.of());
    }

    public AiPageObjectSpecAgent(
            AiPageObjectSpecGenerator generator,
            Function<WorkflowState, List<AiPageObjectSpec>> baselineProvider
    ) {
        if (generator == null) {
            throw new IllegalArgumentException("generator cannot be null");
        }
        if (baselineProvider == null) {
            throw new IllegalArgumentException("baselineProvider cannot be null");
        }
        this.generator = generator;
        this.baselineProvider = baselineProvider;
    }

    @Override
    public String name() {
        return "ai-page-object-spec-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.UI_TEST_PLAN,
                WorkflowArtifact.AI_CONTEXT_PACKAGE,
                WorkflowArtifact.UI_EVIDENCE_FUNNEL_REPORT
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(
                WorkflowArtifact.AI_PAGE_OBJECT_SPECS,
                WorkflowArtifact.POM_CONTRACT_SPECS
        );
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.AI_CONTEXT_PACKAGE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.AI_PAGE_OBJECT_SPECS;
    }

    @Override
    public AiPageObjectSpecInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        UiEvidenceFunnelReport funnelReport = store.require(WorkflowArtifact.UI_EVIDENCE_FUNNEL_REPORT);
        List<AiPageObjectSpec> baselineSpecs = state.getAiPageObjectSpecs().isEmpty()
                ? baselineProvider.apply(state)
                : state.getAiPageObjectSpecs();
        return new AiPageObjectSpecInput(
                WorkflowRunEnvelope.from(state),
                state.getUiTestPlan(),
                state.getAiContextPackage(),
                funnelReport,
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
        );
    }

    @Override
    public boolean supports(AiPageObjectSpecInput input, WorkflowRunEnvelope run) {
        return input != null && input.uiTestPlan() != null && input.aiContextPackage() != null
                && input.evidenceFunnelReport() != null;
    }

    @Override
    public AiPageObjectGenerationResult execute(AiPageObjectSpecInput input, WorkflowRunEnvelope run) {
        if (input.evidenceFunnelReport().metrics().promptEligiblePages() == 0) {
            return new AiPageObjectGenerationResult(
                    List.of(),
                    List.of(),
                    List.of(),
                    java.util.Map.of(
                            "openai.page.object.status", "skipped-no-prompt-eligible-pages",
                            "openai.page.object.scoped.requests", "0",
                            "openai.page.object.llm.attempts", "0",
                            "openai.page.object.llm.successes", "0",
                            "pom.contract.spec.count", "0"
                    ),
                    List.of("Skipped POM prompt and LLM generation because the evidence funnel has zero prompt-eligible pages")
            );
        }
        return generator.generate(new AiPageObjectGenerationRequest(
                input.runEnvelope(),
                input.aiContextPackage(),
                input.uiTestPlan(),
                input.baselineSpecs(),
                input.qualitySummaryInput(),
                input.pipelineSnapshot(),
                input.artifacts()
        ));
    }

    @Override
    public void applyOutput(AiPageObjectGenerationResult output, WorkflowState state) {
        outputPublisher.publishAiPageObjectGenerationResult(output, state);
    }
}
