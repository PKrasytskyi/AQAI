package ua.demo.agentlab.testcase.model;

import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.UiScenarioPrerequisite;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.UiOperationIntent;

import java.util.List;

public record CanonicalTestCase(
        String id,
        String title,
        List<String> requirementRefs,
        List<String> llmSteps,
        List<UiOperationIntent> operationIntents,
        List<AssertionIntent> assertionIntents,
        List<String> targetPages,
        UiScenarioPrerequisite prerequisiteFlow,
        String canonicalFlowId,
        String canonicalFlowType,
        String sourcePageName,
        String pageName,
        String sourceRoute,
        String route,
        String precondition,
        UiAssertionProfile assertionProfile,
        List<String> actions,
        List<String> assertions,
        List<LocatorHint> locatorHints,
        String sourceReference
) {
    public CanonicalTestCase {
        requirementRefs = requirementRefs == null ? List.of() : List.copyOf(requirementRefs);
        llmSteps = llmSteps == null ? List.of() : List.copyOf(llmSteps);
        operationIntents = operationIntents == null ? List.of() : List.copyOf(operationIntents);
        assertionIntents = assertionIntents == null ? List.of() : List.copyOf(assertionIntents);
        targetPages = targetPages == null ? List.of() : List.copyOf(targetPages);
        prerequisiteFlow = prerequisiteFlow == null
                ? new UiScenarioPrerequisite(sourcePageName, sourceRoute, false, List.of())
                : prerequisiteFlow;
        assertionProfile = assertionProfile == null ? UiAssertionProfile.BASIC : assertionProfile;
        actions = actions == null ? List.of() : List.copyOf(actions);
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
        locatorHints = locatorHints == null ? List.of() : List.copyOf(locatorHints);
    }
}
