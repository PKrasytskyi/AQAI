package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

public record SpaSmokeEvidenceFeedbackResult(boolean executed, boolean liveSmokePassed, int locatorsLinked,
                                             int actionsLinked, String details, List<String> sourceTrace) {
    public SpaSmokeEvidenceFeedbackResult {
        details = details == null ? "" : details.trim();
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }
    public static SpaSmokeEvidenceFeedbackResult skipped(String reason) {
        return new SpaSmokeEvidenceFeedbackResult(false, false, 0, 0, reason, List.of("spa-smoke-feedback:skipped"));
    }
}
