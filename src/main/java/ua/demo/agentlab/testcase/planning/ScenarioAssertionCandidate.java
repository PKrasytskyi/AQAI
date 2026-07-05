package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;

public record ScenarioAssertionCandidate(
        AssertionIntentKind intentKind,
        AssertionType assertionType,
        String expectedValue,
        String ownerPage,
        String route,
        String sourceRequirementId
) {
    public ScenarioAssertionCandidate {
        intentKind = intentKind == null ? AssertionIntentKind.CONTENT_VISIBLE : intentKind;
        assertionType = assertionType == null ? AssertionType.ELEMENT_VISIBLE : assertionType;
        expectedValue = expectedValue == null ? "" : expectedValue.trim();
        ownerPage = ownerPage == null ? "" : ownerPage.trim();
        route = route == null ? "" : route.trim();
        sourceRequirementId = sourceRequirementId == null ? "" : sourceRequirementId.trim();
    }
}
