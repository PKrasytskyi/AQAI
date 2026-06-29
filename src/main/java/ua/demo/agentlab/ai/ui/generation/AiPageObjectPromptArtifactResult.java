package ua.demo.agentlab.ai.ui.generation;

import java.util.List;
import java.util.Map;

public record AiPageObjectPromptArtifactResult(
        List<String> artifactFiles,
        Map<String, String> artifacts
) {
    public AiPageObjectPromptArtifactResult {
        artifactFiles = artifactFiles == null ? List.of() : List.copyOf(artifactFiles);
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
    }
}
