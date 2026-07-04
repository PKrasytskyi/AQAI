package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.runtime.RuntimeEvidenceCollector;
import ua.demo.agentlab.ui.discovery.runtime.RuntimeEvidenceArtifactWriter;
import ua.demo.agentlab.ui.discovery.runtime.SeleniumLogRuntimeEvidenceCollector;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.util.Set;

public class UiRuntimeEvidenceAgent implements WorkflowAgent,
        PipelineAgent<SeleniumDiscoveryResult, RuntimeEvidenceBundle> {

    private final RuntimeEvidenceCollector runtimeEvidenceCollector;
    private final RuntimeEvidenceArtifactWriter artifactWriter;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public UiRuntimeEvidenceAgent() {
        this(new SeleniumLogRuntimeEvidenceCollector(), new RuntimeEvidenceArtifactWriter());
    }

    public UiRuntimeEvidenceAgent(
            RuntimeEvidenceCollector runtimeEvidenceCollector,
            RuntimeEvidenceArtifactWriter artifactWriter
    ) {
        this.runtimeEvidenceCollector = runtimeEvidenceCollector == null
                ? new SeleniumLogRuntimeEvidenceCollector()
                : runtimeEvidenceCollector;
        this.artifactWriter = artifactWriter == null ? new RuntimeEvidenceArtifactWriter() : artifactWriter;
    }

    @Override
    public String name() {
        return "ui-runtime-evidence-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.SELENIUM_DISCOVERY_RESULT);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.UI_RUNTIME_EVIDENCE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.SELENIUM_DISCOVERY_RESULT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.UI_RUNTIME_EVIDENCE;
    }

    @Override
    public SeleniumDiscoveryResult inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return state.getSeleniumDiscoveryResult();
    }

    @Override
    public boolean supports(SeleniumDiscoveryResult input, WorkflowRunEnvelope run) {
        return input != null && !input.pages().isEmpty();
    }

    @Override
    public RuntimeEvidenceBundle execute(SeleniumDiscoveryResult input, WorkflowRunEnvelope run) {
        return runtimeEvidenceCollector.collect(input);
    }

    @Override
    public void applyOutput(RuntimeEvidenceBundle output, WorkflowState state) {
        outputPublisher.publishRuntimeEvidence(output, state, artifactWriter);
    }
}
