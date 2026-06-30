package ua.demo.agentlab.ai.debug;

import java.util.List;

public record AiPromptTraceArtifact(
        AiPromptTraceSnapshot snapshot,
        List<String> artifactFiles
) {
    public AiPromptTraceArtifact {
        artifactFiles = artifactFiles == null ? List.of() : List.copyOf(artifactFiles);
    }
}
