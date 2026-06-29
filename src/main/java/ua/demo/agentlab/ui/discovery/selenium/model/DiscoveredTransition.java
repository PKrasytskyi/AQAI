package ua.demo.agentlab.ui.discovery.selenium.model;

public record DiscoveredTransition(
        String fromPageId,
        String actionLabel,
        String actionType,
        String toPageId,
        String toUrl,
        boolean success
) {
}
