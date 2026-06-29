package ua.demo.agentlab.ai.quality;

import ua.demo.agentlab.ai.artifactdiff.ArtifactDiffReport;
import ua.demo.agentlab.ai.artifactdiff.ArtifactDiffService;

import java.nio.file.Path;

public class AiRunArtifactDiffWriter {

    private final ArtifactDiffService artifactDiffService;

    public AiRunArtifactDiffWriter() {
        this(new ArtifactDiffService());
    }

    AiRunArtifactDiffWriter(ArtifactDiffService artifactDiffService) {
        if (artifactDiffService == null) {
            throw new IllegalArgumentException("artifactDiffService cannot be null");
        }
        this.artifactDiffService = artifactDiffService;
    }

    public AiRunArtifactDiffResult write(AiRunQualitySummary summary) {
        ArtifactDiffReport report = artifactDiffService.diffAndArchive(summary);
        String path = Path.of("target", "ai-run", "quality", "artifact-diff.json")
                .toAbsolutePath()
                .normalize()
                .toString();
        return new AiRunArtifactDiffResult(report, path);
    }
}
