package ua.demo.agentlab.artifactreuse.planner;

import java.util.List;

public record RequirementReusePlan(
        String requirementId,
        String targetPage,
        String targetRoute,
        List<ReusePrecondition> preconditions,
        List<ReuseMissingKnowledge> missingKnowledge,
        List<String> candidateFlowIds,
        List<String> notes
) {
    public RequirementReusePlan {
        requirementId = requirementId == null ? "" : requirementId.trim();
        targetPage = targetPage == null ? "" : targetPage.trim();
        targetRoute = targetRoute == null ? "" : targetRoute.trim();
        preconditions = preconditions == null ? List.of() : List.copyOf(preconditions);
        missingKnowledge = missingKnowledge == null ? List.of() : List.copyOf(missingKnowledge);
        candidateFlowIds = candidateFlowIds == null ? List.of() : List.copyOf(candidateFlowIds);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
