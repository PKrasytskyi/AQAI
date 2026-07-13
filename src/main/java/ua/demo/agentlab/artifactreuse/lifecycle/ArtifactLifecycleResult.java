package ua.demo.agentlab.artifactreuse.lifecycle;

import java.util.List;

public record ArtifactLifecycleResult(
        int artifactsEvaluated,
        int stableArtifacts,
        int needsReviewArtifacts,
        List<ArtifactLifecycleEntry> entries
) {
    public ArtifactLifecycleResult {
        artifactsEvaluated = Math.max(0, artifactsEvaluated);
        stableArtifacts = Math.max(0, stableArtifacts);
        needsReviewArtifacts = Math.max(0, needsReviewArtifacts);
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static ArtifactLifecycleResult empty() {
        return new ArtifactLifecycleResult(0, 0, 0, List.of());
    }
}
