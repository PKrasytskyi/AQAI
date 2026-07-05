package ua.demo.agentlab.testcase.planning;

import java.util.List;

public record ScenarioQualityReport(
        boolean valid,
        List<String> issues
) {
    public ScenarioQualityReport {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
