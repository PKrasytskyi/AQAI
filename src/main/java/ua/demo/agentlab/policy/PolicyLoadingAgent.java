package ua.demo.agentlab.policy;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.requirements.model.RequirementInput;

import java.util.Set;

public class PolicyLoadingAgent implements WorkflowAgent,
        PipelineAgent<RequirementInput, GenerationPolicy> {

    private final PolicyResolver policyResolver;
    private final String policyId;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public PolicyLoadingAgent(PolicyResolver policyResolver){
        this(policyResolver, null);
    }

    public PolicyLoadingAgent(PolicyResolver policyResolver, String policyId){
        this.policyResolver = policyResolver;
        this.policyId = policyId;
    }

    @Override
    public String name() {
        return "policy-loading-agent";
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.GENERATION_POLICY);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.REQUIREMENT_INPUT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.GENERATION_POLICY;
    }

    @Override
    public GenerationPolicy execute(RequirementInput input, WorkflowRunEnvelope run) {
        return policyId == null || policyId.isBlank()
                ? policyResolver.resolveDefault() : policyResolver.resolve(policyId);
    }

    @Override
    public void applyOutput(GenerationPolicy output, WorkflowState state) {
        outputPublisher.publishGenerationPolicy(output, state);
    }
}
