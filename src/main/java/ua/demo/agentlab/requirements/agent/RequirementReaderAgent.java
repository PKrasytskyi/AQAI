package ua.demo.agentlab.requirements.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.source.RequirementSource;

import java.util.List;
import java.util.Set;

public class RequirementReaderAgent implements WorkflowAgent,
        PipelineAgent<RequirementInput, RequirementDocument> {

    private final List<RequirementSource> requirementSources;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public RequirementReaderAgent(List<RequirementSource> requirementSources) {
        this.requirementSources = requirementSources;
    }

    @Override
    public String name() {
        return "requirement-reader";
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.REQUIREMENT_INPUT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.REQUIREMENT_DOCUMENT;
    }

    @Override
    public RequirementDocument execute(RequirementInput input, WorkflowRunEnvelope run) {
        RequirementDocument document = requirementSources.stream()
                .filter(source -> source.supports(input))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No RequirementSource found"))
                .load(input);
        return document;
    }

    @Override
    public void applyOutput(RequirementDocument output, WorkflowState state) {
        outputPublisher.publishRequirementDocument(output, state);
    }
}
