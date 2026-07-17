package ua.demo.agentlab.ui.discovery.evidence.funnel;

import java.util.ArrayList;
import java.util.List;

/** Blocks silent evidence loss while allowing explicit, actionable needs-review outcomes. */
public final class UiEvidenceFunnelAcceptanceGate {

    public List<String> validate(UiEvidenceFunnelReport report) {
        List<String> issues = new ArrayList<>();
        if (report == null) {
            return List.of("UI evidence funnel report is missing");
        }
        if (report.requirements().isEmpty()) {
            issues.add("UI evidence funnel contains no requirement results");
        }
        if (!report.completenessPassed()) {
            issues.add("UI evidence funnel completeness status is false");
        }
        for (UiEvidenceRequirementResult result : report.requirements()) {
            if (result.confirmedEvidencePath()) {
                if (!result.promptEligible() || result.evidencePath().isEmpty()) {
                    issues.add(result.requirementId() + ": confirmed path is missing prompt eligibility or trace");
                }
                continue;
            }
            if (!result.hasExplicitStop()) {
                issues.add(result.requirementId() + ": missing stoppedAt, reason, or remediation");
            }
        }
        return List.copyOf(issues);
    }

    public void enforce(UiEvidenceFunnelReport report) {
        List<String> issues = validate(report);
        if (!issues.isEmpty()) {
            throw new IllegalStateException("UI evidence acceptance gate failed with "
                    + issues.size() + " issue(s): " + String.join("; ", issues));
        }
    }
}
