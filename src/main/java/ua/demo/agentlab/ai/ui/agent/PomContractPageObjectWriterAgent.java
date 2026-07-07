package ua.demo.agentlab.ai.ui.agent;

import ua.demo.agentlab.ai.ui.contract.DeterministicPomJavaWriter;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
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

public class PomContractPageObjectWriterAgent implements WorkflowAgent,
        PipelineAgent<List<PomContractSpec>, List<GeneratedSourceFile>> {

    private final DeterministicPomJavaWriter writer;
    private final StageOutputPublisher publisher = new StageOutputPublisher();

    public PomContractPageObjectWriterAgent(DeterministicPomJavaWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("writer cannot be null");
        }
        this.writer = writer;
    }

    @Override
    public String name() {
        return "pom-contract-page-object-writer-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.POM_CONTRACT_SPECS);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES, WorkflowArtifact.PAGE_OBJECT_FILES);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.POM_CONTRACT_SPECS;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PomContractSpec> inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return (List<PomContractSpec>) store.get(WorkflowArtifact.POM_CONTRACT_SPECS).orElse(List.of());
    }

    @Override
    public boolean supports(List<PomContractSpec> input, WorkflowRunEnvelope run) {
        return input != null && !input.isEmpty();
    }

    @Override
    public List<GeneratedSourceFile> execute(List<PomContractSpec> input, WorkflowRunEnvelope run) {
        return writer.write(input);
    }

    @Override
    public void applyOutput(List<GeneratedSourceFile> output, WorkflowState state) {
        publisher.publishAiPageObjectFiles(output, state);
    }
}
