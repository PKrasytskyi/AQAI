package ua.demo.agentlab.ai.quality;

import ua.demo.agentlab.ai.artifactdiff.ArtifactDiffReport;

public record AiRunArtifactDiffResult(
        ArtifactDiffReport report,
        String artifactFile
) {
}
