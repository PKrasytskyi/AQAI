package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelArtifactWriter;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelBuilder;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;

import java.util.Set;

public class UiPageModelAgent implements WorkflowAgent,
        PipelineAgent<UiPageModelInput, PageModelBundle> {

    private final PageModelBuilder pageModelBuilder;
    private final PageModelArtifactWriter artifactWriter;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public UiPageModelAgent(PageModelBuilder pageModelBuilder, PageModelArtifactWriter artifactWriter) {
        if (pageModelBuilder == null) {
            throw new IllegalArgumentException("pageModelBuilder cannot be null");
        }
        if (artifactWriter == null) {
            throw new IllegalArgumentException("artifactWriter cannot be null");
        }
        this.pageModelBuilder = pageModelBuilder;
        this.artifactWriter = artifactWriter;
    }

    @Override
    public String name() {
        return "ui-page-model-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.UI_DISCOVERY_SNAPSHOT);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.PAGE_MODEL_BUNDLE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.UI_DISCOVERY_SNAPSHOT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.PAGE_MODEL_BUNDLE;
    }

    @Override
    public UiPageModelInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return new UiPageModelInput(
                state.getUiDiscoverySnapshot(),
                state.getSeleniumDiscoveryResult()
        );
    }

    @Override
    public boolean supports(UiPageModelInput input, WorkflowRunEnvelope run) {
        return input != null && input.discoverySnapshot() != null;
    }

    @Override
    public PageModelBundle execute(UiPageModelInput input, WorkflowRunEnvelope run) {
        if (input == null || input.discoverySnapshot() == null) {
            throw new IllegalArgumentException("UI discovery snapshot cannot be null");
        }
        return pageModelBuilder.build(input.discoverySnapshot(), input.seleniumDiscoveryResult());
    }

    @Override
    public void applyOutput(PageModelBundle output, WorkflowState state) {
        outputPublisher.publishPageModelBundle(output, state, artifactWriter);
    }
}
