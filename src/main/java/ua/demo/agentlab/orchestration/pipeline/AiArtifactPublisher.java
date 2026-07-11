package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.ai.debug.AiRunArtifactWriter;
import ua.demo.agentlab.orchestration.WorkflowState;

public class AiArtifactPublisher {

    private final AiRunArtifactWriter artifactWriter;

    public AiArtifactPublisher() {
        this(new AiRunArtifactWriter());
    }

    AiArtifactPublisher(AiRunArtifactWriter artifactWriter) {
        if (artifactWriter == null) {
            throw new IllegalArgumentException("artifactWriter cannot be null");
        }
        this.artifactWriter = artifactWriter;
    }

    public void writeJson(WorkflowState state, String stage, String fileName, Object payload) {
        if (state == null) {
            return;
        }
        state.addAiArtifactFile(artifactWriter.writeJson(stage, fileName, payload).toString());
    }

    public void writeText(WorkflowState state, String stage, String fileName, String content) {
        if (state == null) {
            return;
        }
        state.addAiArtifactFile(artifactWriter.writeText(stage, fileName, content).toString());
    }

    public void writeDebugJson(WorkflowState state, String stage, String fileName, Object payload) {
        if (state == null) {
            return;
        }
        artifactWriter.writeDebugJson(stage, fileName, payload)
                .map(java.nio.file.Path::toString)
                .ifPresent(state::addAiArtifactFile);
    }

    public void writeDebugText(WorkflowState state, String stage, String fileName, String content) {
        if (state == null) {
            return;
        }
        artifactWriter.writeDebugText(stage, fileName, content)
                .map(java.nio.file.Path::toString)
                .ifPresent(state::addAiArtifactFile);
    }

    public void register(WorkflowState state, String artifactFile) {
        if (state == null || artifactFile == null || artifactFile.isBlank()) {
            return;
        }
        state.addAiArtifactFile(artifactFile);
    }
}
