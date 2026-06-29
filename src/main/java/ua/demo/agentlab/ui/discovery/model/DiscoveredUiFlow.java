package ua.demo.agentlab.ui.discovery.model;

import java.util.List;

public record DiscoveredUiFlow(
        String flowId,
        String flowName,
        String flowType,
        String sourcePageName,
        String sourceRoute,
        String targetPageName,
        String targetRoute,
        boolean authenticationRequired,
        List<String> stepDescriptions,
        List<String> expectedOutcomes,
        List<String> matchKeywords,
        List<String> sourceRequirementIds
) {
}
