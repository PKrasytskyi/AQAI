package ua.demo.agentlab.ui.discovery.interaction.persistence;

import java.util.List;

public record InteractionGraphProjectionResult(
        boolean executed,
        int actions,
        int locators,
        int relationships,
        String details,
        List<String> findings
) {
    public InteractionGraphProjectionResult {
        actions = Math.max(0, actions);
        locators = Math.max(0, locators);
        relationships = Math.max(0, relationships);
        details = details == null ? "" : details.trim();
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public static InteractionGraphProjectionResult skipped(String reason) {
        return new InteractionGraphProjectionResult(false, 0, 0, 0, reason, List.of(reason));
    }
}
