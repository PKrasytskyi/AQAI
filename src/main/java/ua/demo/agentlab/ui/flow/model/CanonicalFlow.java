package ua.demo.agentlab.ui.flow.model;

import java.util.List;

public record CanonicalFlow(
        String flowId,
        String flowName,
        String flowType,
        String sourcePageName,
        String sourceRoute,
        String targetPageName,
        String targetRoute,
        boolean authenticationRequired,
        List<CanonicalFlowStep> steps,
        List<String> expectedOutcomes,
        List<String> matchKeywords,
        List<String> sourceRequirementIds
) {
}
