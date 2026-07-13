package ua.demo.agentlab.artifactreuse.planner;

import java.util.List;

public record FlowSemanticCandidateBundle(
        List<FlowSemanticCandidate> candidates,
        boolean qdrantHit,
        boolean neo4jHit,
        String vectorUnavailableReason,
        List<String> notes
) {
    public FlowSemanticCandidateBundle {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        vectorUnavailableReason = vectorUnavailableReason == null ? "" : vectorUnavailableReason.trim();
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    public static FlowSemanticCandidateBundle unavailable(String reason) {
        return new FlowSemanticCandidateBundle(List.of(), false, false, reason, List.of());
    }
}
