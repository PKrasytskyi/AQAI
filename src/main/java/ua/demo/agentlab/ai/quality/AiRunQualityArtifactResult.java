package ua.demo.agentlab.ai.quality;

import ua.demo.agentlab.ai.artifactdiff.ArtifactDiffReport;

import java.util.List;
import java.util.Map;

public record AiRunQualityArtifactResult(
        AiRunQualitySummary summary,
        ArtifactDiffReport diffReport,
        List<String> artifactFiles,
        Map<String, String> artifacts
) {
    public AiRunQualityArtifactResult {
        artifactFiles = artifactFiles == null ? List.of() : List.copyOf(artifactFiles);
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
    }
}
