package ua.demo.agentlab.ui.testcontract.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractBundle;
import ua.demo.agentlab.ui.testcontract.validation.UiTestContractValidationResult;
import ua.demo.agentlab.ui.testcontract.writer.DeterministicTestNgGenerationResult;
import ua.demo.agentlab.ui.testcontract.writer.DeterministicTestNgWriter;

import java.util.Set;

public class DeterministicTestNgWriterAgent implements WorkflowAgent,
        PipelineAgent<DeterministicTestNgWriterAgent.Input, DeterministicTestNgGenerationResult> {

    private final DeterministicTestNgWriter writer;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public DeterministicTestNgWriterAgent(DeterministicTestNgWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("writer cannot be null");
        }
        this.writer = writer;
    }

    @Override
    public String name() {
        return "deterministic-testng-writer-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.UI_TEST_CONTRACT_BUNDLE,
                WorkflowArtifact.UI_TEST_CONTRACT_VALIDATION,
                WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(
                WorkflowArtifact.GENERATED_UI_TEST_SOURCES,
                WorkflowArtifact.UI_TEST_FILES,
                WorkflowArtifact.UI_TEST_SOURCE_MAP
        );
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.UI_TEST_CONTRACT_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.GENERATED_UI_TEST_SOURCES;
    }

    @Override
    public Input inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new Input(
                store.require(WorkflowArtifact.UI_TEST_CONTRACT_BUNDLE),
                store.require(WorkflowArtifact.UI_TEST_CONTRACT_VALIDATION)
        );
    }

    @Override
    public DeterministicTestNgGenerationResult execute(Input input, WorkflowRunEnvelope run) {
        if (!input.validation().valid()) {
            throw new IllegalStateException(
                    "Typed UI test contract gate failed with "
                            + input.validation().qualityReport().issues().size() + " issue(s)"
            );
        }
        return writer.write(input.bundle());
    }

    @Override
    public void applyOutput(DeterministicTestNgGenerationResult output, WorkflowState state) {
        outputPublisher.publishDeterministicUiTestFiles(output.files(), state);
        artifactPublisher.writeJson(state, "validation", "ui-test-source-map.json", output.sourceMap());
        for (var file : output.files()) {
            artifactPublisher.writeText(state, "test-spec/generated", file.className() + ".java", file.content());
        }
    }

    public record Input(
            UiTestContractBundle bundle,
            UiTestContractValidationResult validation
    ) {
    }
}
