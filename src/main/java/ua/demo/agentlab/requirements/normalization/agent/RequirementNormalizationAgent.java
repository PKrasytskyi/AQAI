package ua.demo.agentlab.requirements.normalization.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.normalization.RequirementNormalizer;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

import java.util.Set;

public class RequirementNormalizationAgent implements WorkflowAgent,
        PipelineAgent<RequirementDocument, NormalizedRequirementBundle> {

    private final RequirementNormalizer normalizer;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public RequirementNormalizationAgent(RequirementNormalizer normalizer){
        this.normalizer = normalizer;
    }

    @Override
    public String name() {
        return "requirement-normalization-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.REQUIREMENT_DOCUMENT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE;
    }

    @Override
    public void applyOutput(NormalizedRequirementBundle bundle, WorkflowState state) {
        outputPublisher.publishNormalizedRequirementBundle(bundle, state);
    }

    @Override
    public NormalizedRequirementBundle execute(RequirementDocument input, WorkflowRunEnvelope run) {
        if (input == null) {
            throw new IllegalArgumentException("requirement document cannot be null");
        }
        return normalizer.normalize(input);
    }
}
