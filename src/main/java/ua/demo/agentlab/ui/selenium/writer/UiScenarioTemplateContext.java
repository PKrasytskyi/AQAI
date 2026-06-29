package ua.demo.agentlab.ui.selenium.writer;

import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.UiOperationIntent;

import java.util.List;

public record UiScenarioTemplateContext(
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

    private static String defaultValue(String value, String fallback){
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String blankToNull(String value){
        return value == null || value.isBlank() ? null : value;
    }

    public boolean hasExpectedUrlFragment(){
        return expectedUrlFragment != null;
    }
}
