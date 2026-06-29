package ua.demo.agentlab.ui.flow.model;

public record CanonicalFlowStep(
        int order,
        String action,
        String targetPageName
) {
}
