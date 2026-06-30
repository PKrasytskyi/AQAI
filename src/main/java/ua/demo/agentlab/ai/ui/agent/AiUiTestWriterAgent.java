package ua.demo.agentlab.ai.ui.agent;

import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;
import ua.demo.agentlab.ai.ui.writer.AiUiTestTemplateWriter;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;
import java.util.Set;

public class AiUiTestWriterAgent implements WorkflowAgent,
        PipelineAgent<List<AiUiTestSpec>, List<GeneratedSourceFile>> {

    private final AiUiTestTemplateWriter aiWriter;
    private final StageOutputPublisher publisher = new StageOutputPublisher();

    public AiUiTestWriterAgent(AiUiTestTemplateWriter aiWriter) {
        if (aiWriter == null) {
            throw new IllegalArgumentException("writers cannot be null");
        }
        this.aiWriter = aiWriter;
    }

    @Override
    public String name() {
        return "ai-ui-test-writer-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.AI_UI_TEST_SPECS, WorkflowArtifact.PAGE_OBJECT_FILES);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.UI_TEST_FILES);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.AI_UI_TEST_SPECS;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.UI_TEST_FILES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AiUiTestSpec> inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return (List<AiUiTestSpec>) store.get(WorkflowArtifact.AI_UI_TEST_SPECS).orElse(List.of());
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return store != null
                && store.get(WorkflowArtifact.PAGE_OBJECT_FILES).isPresent()
                && store.get(WorkflowArtifact.UI_TEST_FILES).isEmpty();
    }

    @Override
    public List<GeneratedSourceFile> execute(List<AiUiTestSpec> input, WorkflowRunEnvelope run) {
        return input == null || input.isEmpty() ? List.of() : aiWriter.write(input);
    }

    @Override
    public void applyOutput(List<GeneratedSourceFile> output, WorkflowState state) {
        publisher.publishAiUiTestFiles(output, state);
    }
}
