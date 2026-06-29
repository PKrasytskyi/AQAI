package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;

import java.util.List;
import java.util.Map;

public record AiPageObjectGenerationResult(
        List<AiPageObjectSpec> specs,
        List<String> artifactFiles,
        Map<String, String> artifacts,
        List<String> findings
) {
    public AiPageObjectGenerationResult {
        specs = specs == null ? List.of() : List.copyOf(specs);
        artifactFiles = artifactFiles == null ? List.of() : List.copyOf(artifactFiles);
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public static AiPageObjectGenerationResult empty() {
        return new AiPageObjectGenerationResult(List.of(), List.of(), Map.of(), List.of());
    }
}
