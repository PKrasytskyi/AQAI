package ua.demo.agentlab.ui.discovery.catalog;

import java.util.List;

public record LocatorCandidateCoverageReport(
        String schemaVersion,
        String runId,
        int requiredElementCount,
        int elementsWithCandidates,
        int elementsWithLiveVerifiedCandidates,
        int confirmedPrimaryLocators,
        boolean candidateCoveragePassed,
        boolean confirmedLocatorPolicyPassed,
        boolean acceptancePassed,
        List<LocatorElementCoverage> elements,
        List<String> findings
) {
    public static final String SCHEMA_VERSION = "locator-candidate-coverage.v1";

    public LocatorCandidateCoverageReport {
        schemaVersion = safe(schemaVersion).isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        runId = safe(runId);
        requiredElementCount = Math.max(0, requiredElementCount);
        elementsWithCandidates = Math.max(0, elementsWithCandidates);
        elementsWithLiveVerifiedCandidates = Math.max(0, elementsWithLiveVerifiedCandidates);
        confirmedPrimaryLocators = Math.max(0, confirmedPrimaryLocators);
        elements = elements == null ? List.of() : List.copyOf(elements);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
