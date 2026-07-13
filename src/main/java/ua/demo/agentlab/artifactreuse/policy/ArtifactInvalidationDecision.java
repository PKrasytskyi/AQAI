package ua.demo.agentlab.artifactreuse.policy;

import java.util.List;

public record ArtifactInvalidationDecision(boolean reusable, List<ArtifactInvalidationReason> reasons) {
    public ArtifactInvalidationDecision {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
