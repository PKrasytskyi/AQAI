package ua.demo.agentlab.templates.ui;

import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.UiOperationIntent;

import java.util.List;

public record UiScenarioTemplateContext(
        String sourcePageName,
        String sourceRoute,
        boolean authenticationRequired,
        String assertionProfile,
        String sourcePageVariableName,
        String sourceOpenMethodName,
        String pageVariableName,
        String openMethodName,
        String primaryActionMethodName,
        String credentialsProfileName,
        String dataSetName,
        String expectedUrlFragment,
        String successStateMethodName,
        String errorStateMethodName,
        String emptyStateMethodName,
        String detailsStateMethodName,
        List<UiOperationIntent> operationIntents,
        List<AssertionIntent> assertionIntents,
        List<String> scenarioActions,
        List<String> scenarioAssertions
) {

    public UiScenarioTemplateContext {
        sourcePageName = blankToNull(sourcePageName);
        sourceRoute = blankToNull(sourceRoute);
        assertionProfile = defaultValue(assertionProfile, "BASIC");
        sourcePageVariableName = defaultValue(sourcePageVariableName, "page");
        sourceOpenMethodName = defaultValue(sourceOpenMethodName, "open");
        pageVariableName = defaultValue(pageVariableName, "page");
        openMethodName = defaultValue(openMethodName, "open");
        primaryActionMethodName = defaultValue(primaryActionMethodName, "performPrimaryAction");
        credentialsProfileName = defaultValue(credentialsProfileName, "valid-user");
        dataSetName = defaultValue(dataSetName, "default");
        successStateMethodName = defaultValue(successStateMethodName, "isSuccessStateVisible");
        errorStateMethodName = defaultValue(errorStateMethodName, "isErrorStateVisible");
        emptyStateMethodName = defaultValue(emptyStateMethodName, "isEmptyStateVisible");
        detailsStateMethodName = defaultValue(detailsStateMethodName, "isDetailsStateVisible");
        expectedUrlFragment = blankToNull(expectedUrlFragment);
        operationIntents = operationIntents == null ? List.of() : List.copyOf(operationIntents);
        assertionIntents = assertionIntents == null ? List.of() : List.copyOf(assertionIntents);
        scenarioActions = scenarioActions == null ? List.of() : List.copyOf(scenarioActions);
        scenarioAssertions = scenarioAssertions == null ? List.of() : List.copyOf(scenarioAssertions);
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public boolean hasExpectedUrlFragment() {
        return expectedUrlFragment != null;
    }

    public boolean hasSourcePage() {
        return sourcePageName != null;
    }

    public String navigationPageVariableName() {
        return sourcePageVariableName;
    }

    public String navigationOpenMethodName() {
        return sourceOpenMethodName;
    }

    public String assertionPageVariableName() {
        return pageVariableName;
    }

    public boolean hasDistinctSourcePage() {
        return sourcePageName != null && !sourcePageVariableName.equals(pageVariableName);
    }
}
