package ua.demo.agentlab.ai.flow;

import ua.demo.agentlab.ui.catalog.PageCapability;

import java.util.List;

public record BusinessFlowContext(
        String flowId,
        String objective,
        List<String> targetPageNames,
        List<String> targetRoutes,
        List<PageCapability> targetCapabilities,
        List<String> targetOperations,
        List<String> requiredTerms,
        List<String> excludedTerms,
        List<String> notes
) {
    public BusinessFlowContext {
        flowId = flowId == null ? "" : flowId.trim();
        objective = objective == null ? "" : objective.trim();
        targetPageNames = targetPageNames == null ? List.of() : List.copyOf(targetPageNames);
        targetRoutes = targetRoutes == null ? List.of() : List.copyOf(targetRoutes);
        targetCapabilities = targetCapabilities == null ? List.of() : List.copyOf(targetCapabilities);
        targetOperations = targetOperations == null ? List.of() : List.copyOf(targetOperations);
        requiredTerms = requiredTerms == null ? List.of() : List.copyOf(requiredTerms);
        excludedTerms = excludedTerms == null ? List.of() : List.copyOf(excludedTerms);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
