package ua.demo.agentlab.artifactreuse.planner;

import ua.demo.agentlab.artifactreuse.flow.FlowContractBundle;
import ua.demo.agentlab.artifactreuse.semantic.FlowSemanticCandidateService;
import ua.demo.agentlab.artifactreuse.semantic.FlowSemanticIndexResult;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

import java.util.Set;

public class FlowSemanticCandidateAgent implements WorkflowAgent, PipelineAgent<FlowSemanticCandidateAgent.Input, FlowSemanticCandidateBundle> {

    private final FlowSemanticCandidateService service;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public FlowSemanticCandidateAgent(FlowSemanticCandidateService service) {
        if (service == null) throw new IllegalArgumentException("flow semantic candidate service cannot be null");
        this.service = service;
    }

    @Override public String name() { return "flow-semantic-candidate-agent"; }
    @Override public Set<WorkflowArtifact> requires() { return Set.of(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE, WorkflowArtifact.FLOW_CONTRACT_BUNDLE, WorkflowArtifact.FLOW_CONTRACT_PERSISTENCE, WorkflowArtifact.FLOW_SEMANTIC_INDEX_RESULT); }
    @Override public Set<WorkflowArtifact> produces() { return Set.of(WorkflowArtifact.FLOW_SEMANTIC_CANDIDATES); }
    @Override public WorkflowArtifact input() { return WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.FLOW_SEMANTIC_CANDIDATES; }

    @Override
    public Input inputFrom(PipelineArtifactStore store, WorkflowState state) {
        CanonicalTestCaseBundle tests = (CanonicalTestCaseBundle) store.get(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE)
                .orElse(state.getCanonicalTestCaseBundle());
        FlowContractBundle flows = (FlowContractBundle) store.get(WorkflowArtifact.FLOW_CONTRACT_BUNDLE)
                .orElse(new FlowContractBundle("flow-contract-bundle.v1", state.getKnowledgeRunMetadata(), java.util.List.of()));
        FlowSemanticIndexResult index = (FlowSemanticIndexResult) store.get(WorkflowArtifact.FLOW_SEMANTIC_INDEX_RESULT)
                .orElse(FlowSemanticIndexResult.skipped("semantic index was not produced"));
        return new Input(tests, flows, index);
    }

    @Override public boolean supports(Input input, WorkflowRunEnvelope run) { return input != null && input.tests() != null; }

    @Override
    public FlowSemanticCandidateBundle execute(Input input, WorkflowRunEnvelope run) {
        if (!input.index().attempted() && !input.index().success()) {
            return FlowSemanticCandidateBundle.unavailable(input.index().message());
        }
        return service.findCandidates(input.tests(), input.flows().runMetadata());
    }

    @Override
    public void applyOutput(FlowSemanticCandidateBundle output, WorkflowState state) {
        if (output == null || state == null) return;
        state.addArtifact("artifact.reuse.semantic.qdrant.hit", String.valueOf(output.qdrantHit()));
        state.addArtifact("artifact.reuse.semantic.neo4j.hit", String.valueOf(output.neo4jHit()));
        state.addArtifact("artifact.reuse.semantic.candidate.count", String.valueOf(output.candidates().size()));
        state.addArtifact("artifact.reuse.semantic.unavailable.reason", output.vectorUnavailableReason());
        artifactPublisher.writeJson(state, "artifact-reuse", "flow-semantic-candidates.json", output);
    }

    public record Input(CanonicalTestCaseBundle tests, FlowContractBundle flows, FlowSemanticIndexResult index) { }
}
