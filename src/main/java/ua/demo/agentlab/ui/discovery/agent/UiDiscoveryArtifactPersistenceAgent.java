package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.discovery.persistence.DiscoveryArtifactWriter;

import java.util.List;

public class UiDiscoveryArtifactPersistenceAgent implements WorkflowAgent {

    private final DiscoveryArtifactWriter discoveryArtifactWriter;

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
    public int order() {
        return 28;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getUiDiscoverySnapshot() != null && state.getDiscoveryArtifactFiles().isEmpty();
    }

    @Override
    public void execute(WorkflowState state) {
        List<String> writtenFiles = discoveryArtifactWriter.write(
                state.getUiDiscoverySnapshot(),
                state.getSeleniumDiscoveryResult(),
                state.getMappedUiKnowledge()
        );

        for (String path : writtenFiles) {
            state.addDiscoveryArtifactFile(path);
        }

        state.addArtifact("ui.discovery.artifact.count", String.valueOf(writtenFiles.size()));
        if (state.getSeleniumDiscoveryResult() != null) {
            long evidenceCount = state.getSeleniumDiscoveryResult().pages().stream()
                    .filter(page -> page.evidence() != null)
                    .count();
            state.addArtifact("ui.discovery.evidence.page.count", String.valueOf(evidenceCount));
            state.addFinding("UI discovery evidence captured for " + evidenceCount + " page(s)");
        }
        if (state.getMappedUiKnowledge() != null) {
            state.addArtifact("ui.discovery.mapped.page.count", String.valueOf(state.getMappedUiKnowledge().pages().size()));
            state.addArtifact("ui.discovery.mapped.vector.document.count",
                    String.valueOf(state.getMappedUiKnowledge().vectorDocuments().size()));
            state.addFinding("Mapped UI knowledge artifacts written for " + state.getMappedUiKnowledge().pages().size() + " page(s)");
        }
        state.addFinding("UI discovery artifacts written: " + writtenFiles.size());
    }
}
