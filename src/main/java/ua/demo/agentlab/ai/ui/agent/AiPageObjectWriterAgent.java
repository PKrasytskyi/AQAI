package ua.demo.agentlab.ai.ui.agent;

import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.writer.AiPageObjectTemplateWriter;
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

public class AiPageObjectWriterAgent implements WorkflowAgent,
        PipelineAgent<List<AiPageObjectSpec>, List<GeneratedSourceFile>> {

    private final AiPageObjectTemplateWriter aiWriter;
    private final StageOutputPublisher publisher = new StageOutputPublisher();

    public AiPageObjectWriterAgent(AiPageObjectTemplateWriter aiWriter) {
        if (aiWriter == null) {
            throw new IllegalArgumentException("writers cannot be null");
        }
        this.aiWriter = aiWriter;
    }

    @Override
    public String name() {
        return "ai-page-object-writer-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.AI_PAGE_OBJECT_SPECS, WorkflowArtifact.UI_TEST_PLAN);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES, WorkflowArtifact.PAGE_OBJECT_FILES);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.AI_PAGE_OBJECT_SPECS;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AiPageObjectSpec> inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return (List<AiPageObjectSpec>) store.get(WorkflowArtifact.AI_PAGE_OBJECT_SPECS).orElse(List.of());
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return store != null
                && store.get(WorkflowArtifact.UI_TEST_PLAN).isPresent()
                && store.get(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES).isEmpty();
    }

    @Override
    public List<GeneratedSourceFile> execute(List<AiPageObjectSpec> input, WorkflowRunEnvelope run) {
        return input == null || input.isEmpty() ? List.of() : aiWriter.write(input);
    }

    @Override
    public void applyOutput(List<GeneratedSourceFile> output, WorkflowState state) {
        publisher.publishAiPageObjectFiles(output, state);
    }
}
