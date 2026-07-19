package ua.demo.agentlab.ui.testcontract.model;

import java.util.List;

public record UiTestContractSpec(
        String scenarioId,
        String capability,
        String className,
        String testMethodName,
        String description,
        String sourcePage,
        String targetPage,
        List<UiTestActionSpec> preconditions,
        List<UiTestActionSpec> actions,
        List<UiTestAssertionSpec> assertions,
        List<UiTestDataReferenceSpec> dataReferences,
        List<String> requirementIds,
        List<String> coverageGaps
) {
    public UiTestContractSpec {
        scenarioId = safe(scenarioId);
        capability = safe(capability);
        className = safe(className);
        testMethodName = safe(testMethodName);
        description = safe(description);
        sourcePage = safe(sourcePage);
        targetPage = safe(targetPage);
        preconditions = preconditions == null ? List.of() : List.copyOf(preconditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
        dataReferences = dataReferences == null ? List.of() : List.copyOf(dataReferences);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        coverageGaps = coverageGaps == null ? List.of() : List.copyOf(coverageGaps);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
