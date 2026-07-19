package ua.demo.agentlab.ui.discovery.evidence.funnel;

import java.util.List;

/** Formats terminal observations; it cannot change pipeline decisions. */
public final class TerminalFailureProjection {
    public List<String> findings(List<UiEvidenceRequirementResult> results) {
        return List.of("requirements=" + results.size(),
                "confirmedEvidencePaths=" + results.stream().filter(UiEvidenceRequirementResult::confirmedEvidencePath).count(),
                "explicitStops=" + results.stream().filter(result -> !result.confirmedEvidencePath()
                        && result.hasExplicitStop()).count());
    }
}
