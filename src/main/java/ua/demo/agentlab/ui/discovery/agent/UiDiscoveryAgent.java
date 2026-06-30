package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.UiDiscoveryService;
import ua.demo.agentlab.ui.discovery.model.UiDiscoveryResult;
import ua.demo.agentlab.ui.flow.CanonicalPageFlowMapper;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

import java.util.Set;

public class UiDiscoveryAgent implements WorkflowAgent,
        PipelineAgent<UiDiscoveryInput, UiDiscoveryOutput> {

    private final UiDiscoveryService discoveryService;
    private final CanonicalPageFlowMapper canonicalMapper;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public UiDiscoveryAgent(UiDiscoveryService discoveryService, CanonicalPageFlowMapper canonicalMapper) {
        if (discoveryService == null) {
            throw new IllegalArgumentException("discoveryService cannot be null");
        }
        if (canonicalMapper == null) {
            throw new IllegalArgumentException("canonicalMapper cannot be null");
        }
        this.discoveryService = discoveryService;
        this.canonicalMapper = canonicalMapper;
    }

    @Override
    public String name() {
        return "ui-discovery-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.PROJECT_PROFILE, WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(
                WorkflowArtifact.UI_DISCOVERY_SNAPSHOT,
                WorkflowArtifact.SELENIUM_DISCOVERY_RESULT,
                WorkflowArtifact.CANONICAL_PAGE_FLOW_MODEL
        );
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.UI_DISCOVERY_SNAPSHOT;
    }

    @Override
    public UiDiscoveryInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return new UiDiscoveryInput(
                state.getProjectProfile(),
                state.getNormalizedRequirementBundle()
        );
    }

    @Override
    public boolean supports(UiDiscoveryInput input, WorkflowRunEnvelope run) {
        return input != null
                && input.projectProfile() != null
                && input.normalizedRequirementBundle() != null;
    }

    @Override
    public UiDiscoveryOutput execute(UiDiscoveryInput input, WorkflowRunEnvelope run) {
        UiDiscoveryResult discoveryResult = discoveryService.discover(
                input.projectProfile(),
                input.normalizedRequirementBundle()
        );
        CanonicalPageFlowModel canonicalModel = canonicalMapper.map(discoveryResult.snapshot());
        return UiDiscoveryOutput.from(discoveryResult, canonicalModel);
    }

    @Override
    public void applyOutput(UiDiscoveryOutput output, WorkflowState state) {
        outputPublisher.publishUiDiscoveryOutput(output, state);
    }
}
