package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;

import java.util.List;
import java.util.Map;

public record AiUiTestGenerationResult(
        List<AiUiTestSpec> specs,
        List<String> artifactFiles,
        Map<String, String> artifacts,
        List<String> findings
) {
    public AiUiTestGenerationResult {
        specs = specs == null ? List.of() : List.copyOf(specs);
        artifactFiles = artifactFiles == null ? List.of() : List.copyOf(artifactFiles);
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public static AiUiTestGenerationResult empty() {
        return new AiUiTestGenerationResult(List.of(), List.of(), Map.of(), List.of());
    }
}
