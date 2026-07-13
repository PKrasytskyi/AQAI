package ua.demo.agentlab.artifactreuse.flow;

import ua.demo.agentlab.artifactreuse.config.ArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.semantic.FlowSemanticIndexer;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;

import java.time.Instant;
import java.util.Set;

/** Persists smoke-derived flow quality after the writer/compile/review/smoke path completes. */
public class FlowRuntimeFeedbackAgent implements WorkflowAgent,
        PipelineAgent<FlowRuntimeFeedbackAgent.Input, FlowRuntimeFeedbackResult> {

    private final ArtifactReuseRuntimeConfig config;
    private final FlowContractRegistry registry;
    private final FlowSemanticIndexer semanticIndexer;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public FlowRuntimeFeedbackAgent(ArtifactReuseRuntimeConfig config, FlowContractRegistry registry) {
        this(config, registry, null);
    }

    public FlowRuntimeFeedbackAgent(
            ArtifactReuseRuntimeConfig config,
            FlowContractRegistry registry,
            FlowSemanticIndexer semanticIndexer
    ) {
        if (config == null || registry == null) throw new IllegalArgumentException("flow runtime feedback dependencies cannot be null");
        this.config = config;
        this.registry = registry;
        this.semanticIndexer = semanticIndexer;
    }

    @Override public String name() { return "flow-runtime-feedback-agent"; }
    @Override public Set<WorkflowArtifact> requires() { return Set.of(WorkflowArtifact.FLOW_CONTRACT_BUNDLE, WorkflowArtifact.GENERATED_UI_SMOKE_RESULT, WorkflowArtifact.ARTIFACT_LIFECYCLE_RESULT); }
    @Override public Set<WorkflowArtifact> produces() { return Set.of(WorkflowArtifact.FLOW_CONTRACT_RUNTIME_FEEDBACK); }
    @Override public WorkflowArtifact input() { return WorkflowArtifact.GENERATED_UI_SMOKE_RESULT; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.FLOW_CONTRACT_RUNTIME_FEEDBACK; }

    @Override
    public Input inputFrom(PipelineArtifactStore store, WorkflowState state) {
        FlowContractBundle flows = (FlowContractBundle) store.get(WorkflowArtifact.FLOW_CONTRACT_BUNDLE)
                .orElse(new FlowContractBundle("flow-contract-bundle.v1", state.getKnowledgeRunMetadata(), java.util.List.of()));
        GeneratedUiSmokeResult smoke = (GeneratedUiSmokeResult) store.get(WorkflowArtifact.GENERATED_UI_SMOKE_RESULT).orElse(null);
        boolean liveEnabled = Boolean.parseBoolean(state.getArtifacts().getOrDefault("generated.ui.live.smoke.enabled", "false"));
        String liveStatus = state.getArtifacts().getOrDefault("generated.ui.live.smoke.status", "SKIPPED");
        return new Input(flows, smoke, liveEnabled, liveStatus);
    }

    @Override public boolean supports(Input input, WorkflowRunEnvelope run) { return input != null && input.smokeResult() != null && !input.flows().contracts().isEmpty(); }

    @Override
    public FlowRuntimeFeedbackResult execute(Input input, WorkflowRunEnvelope run) {
        if (!config.enabled() || !config.flowContractEnabled()) {
            return FlowRuntimeFeedbackResult.skipped("neo4j", "flow contract feedback is disabled");
        }
        if (!input.liveSmokeEnabled()) {
            return FlowRuntimeFeedbackResult.skipped("neo4j", "live smoke is disabled; generated source smoke is not runtime flow evidence");
        }
        boolean passed = input.smokeResult().passed() && "PASSED".equalsIgnoreCase(input.liveStatus());
        String occurredAt = Instant.now().toString();
        FlowContractBundle feedbackBundle = passed ? runtimeVerifiedBundle(input.flows(), occurredAt) : input.flows();
        if (passed && !feedbackBundle.contracts().isEmpty()) {
            registry.persist(feedbackBundle);
        }
        FlowRuntimeFeedbackResult result = registry.recordRuntimeFeedback(
                feedbackBundle, new FlowRuntimeFeedback(passed, "live-ui-smoke", occurredAt));
        if (passed && result.success() && semanticIndexer != null) {
            semanticIndexer.index(feedbackBundle);
        }
        return result;
    }

    @Override
    public void applyOutput(FlowRuntimeFeedbackResult output, WorkflowState state) {
        if (state == null || output == null) return;
        state.addArtifact("artifact.reuse.flow.feedback.attempted", String.valueOf(output.attempted()));
        state.addArtifact("artifact.reuse.flow.feedback.success", String.valueOf(output.success()));
        state.addArtifact("artifact.reuse.flow.feedback.updated", String.valueOf(output.contractsUpdated()));
        state.addArtifact("artifact.reuse.flow.feedback.message", output.message());
        artifactPublisher.writeJson(state, "flow-contracts", "flow-runtime-feedback.json", output);
        String generatedSmokeStatus = state.getArtifacts().getOrDefault("generated.ui.smoke.status", "SKIPPED");
        String liveSmokeStatus = state.getArtifacts().getOrDefault("generated.ui.live.smoke.status", "SKIPPED");
        if (!"PASSED".equalsIgnoreCase(generatedSmokeStatus)) {
            state.fail("Generated UI smoke validation failed after flow feedback persistence: " + generatedSmokeStatus);
        } else if ("FAILED".equalsIgnoreCase(liveSmokeStatus)) {
            state.fail("Generated UI live smoke validation failed after flow feedback persistence: " + liveSmokeStatus);
        }
    }

    public record Input(FlowContractBundle flows, GeneratedUiSmokeResult smokeResult, boolean liveSmokeEnabled, String liveStatus) {
        public Input {
            flows = flows == null ? new FlowContractBundle("flow-contract-bundle.v1", null, java.util.List.of()) : flows;
            liveStatus = liveStatus == null ? "SKIPPED" : liveStatus.trim();
        }
    }

    private FlowContractBundle runtimeVerifiedBundle(FlowContractBundle source, String occurredAt) {
        return new FlowContractBundle(source.schemaVersion(), source.runMetadata(), source.contracts().stream()
                .filter(this::validatedByLiveCapabilitySmoke)
                .map(contract -> new FlowContract(
                        contract.schemaVersion(), contract.flowId(), contract.displayName(), contract.type(), contract.source(),
                        contract.target(), contract.requiresStates(), contract.producesStates(), contract.steps(), contract.assertions(),
                        contract.requirementIds(), contract.evidence(), contract.artifactIds(), contract.contractFingerprint(), occurredAt,
                        1.0d, 0.0d, contract.confidence(), FlowContractStatus.CONFIRMED
                ))
                .toList());
    }

    private boolean validatedByLiveCapabilitySmoke(FlowContract contract) {
        if (contract == null) return false;
        return contract.type() == FlowContractType.AUTHENTICATION
                || contract.type() == FlowContractType.LOGOUT
                || contract.type() == FlowContractType.FORM_ENTRY
                || contract.type() == FlowContractType.FORM_SUBMISSION;
    }
}
