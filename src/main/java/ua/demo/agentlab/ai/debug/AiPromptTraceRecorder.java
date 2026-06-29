package ua.demo.agentlab.ai.debug;

import ua.demo.agentlab.orchestration.WorkflowState;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class AiPromptTraceRecorder {

    private final AiRunArtifactWriter artifactWriter;

    public AiPromptTraceRecorder() {
        this(new AiRunArtifactWriter());
    }

    AiPromptTraceRecorder(AiRunArtifactWriter artifactWriter) {
        if (artifactWriter == null) {
            throw new IllegalArgumentException("artifactWriter cannot be null");
        }
        this.artifactWriter = artifactWriter;
    }

    public AiPromptTraceSnapshot recordPrompt(
            WorkflowState state,
            String stage,
            String fileStem,
            String promptType,
            String scopeId,
            String subject,
            String prompt,
            Map<String, Object> metadata
    ) {
        Path promptPath = artifactWriter.writeText(stage, fileStem + "-prompt.txt", prompt);

        Map<String, Object> traceMetadata = new LinkedHashMap<>();
        if (metadata != null) {
            traceMetadata.putAll(metadata);
        }
        traceMetadata.put("promptLength", prompt == null ? 0 : prompt.length());

        AiPromptTraceSnapshot snapshot = new AiPromptTraceSnapshot(
                stage,
                scopeId,
                subject,
                promptType,
                promptPath.toString(),
                traceMetadata
        );

        Path tracePath = artifactWriter.writeJson(stage, fileStem + "-prompt-trace.json", snapshot);
        if (state != null) {
            state.addAiArtifactFile(promptPath.toString());
            state.addAiArtifactFile(tracePath.toString());
        }

        return snapshot;
    }
}
