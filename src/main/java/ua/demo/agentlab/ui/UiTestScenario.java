package ua.demo.agentlab.ui;

import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.UiOperationIntent;

import java.util.List;

public record UiTestScenario(

        String id,
        String title,
        String canonicalFlowId,
        String canonicalFlowType,
        String sourcePageName,
        String pageName,
        String sourceRoute,
        String route,
        String precondition,
        UiScenarioPrerequisite prerequisite,
        UiAssertionProfile assertionProfile,
        List<String> actions,
        List<String> assertions,
        List<UiOperationIntent> operationIntents,
        List<AssertionIntent> assertionIntents,
        List<LocatorHint> locatorHints,
        String sourceReference
) {

    public UiTestScenario {
        prerequisite = prerequisite == null
                ? new UiScenarioPrerequisite(sourcePageName, sourceRoute, false, List.of())
                : prerequisite;
        assertionProfile = assertionProfile == null ? UiAssertionProfile.BASIC : assertionProfile;
        actions = actions == null ? List.of() : List.copyOf(actions);
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
        operationIntents = operationIntents == null ? List.of() : List.copyOf(operationIntents);
        assertionIntents = assertionIntents == null ? List.of() : List.copyOf(assertionIntents);
        locatorHints = locatorHints == null ? List.of() : List.copyOf(locatorHints);
    }
}
