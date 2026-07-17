package ua.demo.agentlab.ai.expectationenrichment.service;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.expectationenrichment.model.ExpectedResultCandidate;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.UiScenarioPrerequisite;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;

import java.util.List;

public class ExpectedResultConflictDetectorTest {

    @Test
    public void rejectsSearchButtonVisibilityAsSearchResultForLoginRoute() {
        CanonicalTestCase testCase = new CanonicalTestCase(
                "REQ-016", "User can click the Search button", List.of("REQ-016"), List.of(),
                List.of(new UiOperationIntent(UiOperationKind.SEARCH, "LoginPage", null)),
                List.of(new AssertionIntent(AssertionIntentKind.CONTENT_VISIBLE, "User can click the Search button")),
                List.of("LoginPage"), new UiScenarioPrerequisite("LoginPage", "/auth/login", false, List.of()),
                "flow-REQ-016", "SEARCH", "LoginPage", "LoginPage", "/auth/login", "/auth/login",
                "Application is available", UiAssertionProfile.BASIC, List.of("Search"),
                List.of("User can click the Search button"), List.of(), "requirements/vacancies.md [L1]"
        );
        ExpectedResultCandidate candidate = new ExpectedResultCandidate(
                "REQ-022", "Search button is visible and enabled.", "requirements/vacancies.md [L2]"
        );

        Assert.assertFalse(new ExpectedResultConflictDetector().detect(testCase, candidate).isEmpty());
    }
}
