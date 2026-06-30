package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.DiscoveryArtifactWriter;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UiDiscoveryArtifactPersistenceAgent implements WorkflowAgent,
        PipelineAgent<UiDiscoveryArtifactPersistenceInput, UiDiscoveryArtifactPersistenceResult> {

    private final DiscoveryArtifactWriter discoveryArtifactWriter;
    private final StageOutputPublisher publisher = new StageOutputPublisher();

    public UiDiscoveryArtifactPersistenceAgent(DiscoveryArtifactWriter discoveryArtifactWriter) {
        if (discoveryArtifactWriter == null) {
            throw new IllegalArgumentException("discoveryArtifactWriter cannot be null");
        }
        this.discoveryArtifactWriter = discoveryArtifactWriter;
    }

    @Override
    public String name() {
        return "ui-discovery-artifact-persistence-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.UI_DISCOVERY_SNAPSHOT);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.UI_DISCOVERY_ARTIFACT_PERSISTENCE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.UI_DISCOVERY_SNAPSHOT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.UI_DISCOVERY_ARTIFACT_PERSISTENCE;
    }

    @Override
    public UiDiscoveryArtifactPersistenceInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new UiDiscoveryArtifactPersistenceInput(
                store.require(WorkflowArtifact.UI_DISCOVERY_SNAPSHOT),
                store.get(WorkflowArtifact.SELENIUM_DISCOVERY_RESULT)
                        .filter(SeleniumDiscoveryResult.class::isInstance)
                        .map(SeleniumDiscoveryResult.class::cast)
                        .orElse(null),
                store.get(WorkflowArtifact.MAPPED_UI_KNOWLEDGE)
                        .filter(MappedUiKnowledge.class::isInstance)
                        .map(MappedUiKnowledge.class::cast)
                        .orElse(null)
        );
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return store != null
                && store.get(WorkflowArtifact.UI_DISCOVERY_SNAPSHOT).isPresent()
                && store.get(WorkflowArtifact.UI_DISCOVERY_ARTIFACT_PERSISTENCE).isEmpty();
    }

    @Override
    public UiDiscoveryArtifactPersistenceResult execute(
            UiDiscoveryArtifactPersistenceInput input,
            WorkflowRunEnvelope run
    ) {
        List<String> writtenFiles = discoveryArtifactWriter.write(
                input.discoverySnapshot(),
                input.seleniumDiscoveryResult(),
                input.mappedUiKnowledge()
        );

        Map<String, String> artifacts = new LinkedHashMap<>();
        List<String> findings = new ArrayList<>();
        artifacts.put("ui.discovery.artifact.count", String.valueOf(writtenFiles.size()));
        if (input.seleniumDiscoveryResult() != null) {
            long evidenceCount = input.seleniumDiscoveryResult().pages().stream()
                    .filter(page -> page.evidence() != null)
                    .count();
            artifacts.put("ui.discovery.evidence.page.count", String.valueOf(evidenceCount));
            findings.add("UI discovery evidence captured for " + evidenceCount + " page(s)");
        }
        if (input.mappedUiKnowledge() != null) {
            artifacts.put("ui.discovery.mapped.page.count", String.valueOf(input.mappedUiKnowledge().pages().size()));
            artifacts.put("ui.discovery.mapped.vector.document.count",
                    String.valueOf(input.mappedUiKnowledge().vectorDocuments().size()));
            findings.add("Mapped UI knowledge artifacts written for "
                    + input.mappedUiKnowledge().pages().size() + " page(s)");
        }
        findings.add("UI discovery artifacts written: " + writtenFiles.size());
        return new UiDiscoveryArtifactPersistenceResult(writtenFiles, artifacts, findings);
    }

    @Override
    public void applyOutput(UiDiscoveryArtifactPersistenceResult output, WorkflowState state) {
        publisher.publishUiDiscoveryArtifactPersistence(output, state);
    }
}
