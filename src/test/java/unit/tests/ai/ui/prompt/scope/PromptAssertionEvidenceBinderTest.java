package unit.tests.ai.ui.prompt.scope;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.assertions.model.AssertionSource;
import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptAssertionEvidenceBinder;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyLocator;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;

public class PromptAssertionEvidenceBinderTest {

    @Test
    public void bindsBusinessVisibilityTextToConfirmedSemanticLocator() {
        AssertionContract assertion = assertion(AssertionType.ELEMENT_VISIBLE,
                "Username input is visible and enabled");

        var result = new PromptAssertionEvidenceBinder().bind(assertion, "/login",
                List.of(locator("username"), locator("password")));

        Assert.assertEquals(result.assertion().targetLocatorId(), "username");
        Assert.assertTrue(result.coverageGap().isBlank());
    }

    @Test
    public void normalizesAuthenticatedAreaAbsentToConfirmedPublicRouteCheck() {
        AssertionContract assertion = assertion(AssertionType.AUTHENTICATED_AREA_ABSENT,
                "Authenticated area is no longer accessible after logout");

        var result = new PromptAssertionEvidenceBinder().bind(assertion, "/auth/login", List.of());

        Assert.assertEquals(result.assertion().type(), "URL_CONTAINS");
        Assert.assertEquals(result.assertion().expectedValue(), "/auth/login");
        Assert.assertTrue(result.assertion().sourceTrace().contains("normalized-from:AUTHENTICATED_AREA_ABSENT"));
    }

    @Test
    public void doesNotUseMenuTriggerAsEvidenceThatLogoutActionIsVisible() {
        AssertionContract assertion = assertion(AssertionType.AUTHENTICATED_AREA_VISIBLE,
                "The user menu opens and the logout action becomes visible and enabled.");

        var result = new PromptAssertionEvidenceBinder().bind(assertion, "/dashboard/index",
                List.of(locator("userMenuTrigger")));

        Assert.assertEquals(result.assertion().type(), "ELEMENT_VISIBLE");
        Assert.assertTrue(result.assertion().targetLocatorId().isBlank());
        Assert.assertTrue(result.coverageGap().contains("no confirmed target locator"));
    }

    @Test
    public void bindsMenuPostconditionToConfirmedLogoutLocator() {
        AssertionContract assertion = assertion(AssertionType.AUTHENTICATED_AREA_VISIBLE,
                "The user menu opens and the logout action becomes visible and enabled.");

        var result = new PromptAssertionEvidenceBinder().bind(assertion, "/dashboard/index",
                List.of(locator("userMenuTrigger"), locator("logoutLink")));

        Assert.assertEquals(result.assertion().type(), "ELEMENT_VISIBLE");
        Assert.assertEquals(result.assertion().targetLocatorId(), "logoutLink");
        Assert.assertEquals(result.assertion().expectedValue(), "logoutLink");
        Assert.assertTrue(result.coverageGap().isBlank());
    }

    private AssertionContract assertion(AssertionType type, String expected) {
        return new AssertionContract("REQ-1", "TC-1", type, expected, "LoginPage", "/login",
                "fixture.md [L1]", 1.0d, AssertionSource.REQUIREMENT);
    }

    private PromptReadyLocator locator(String id) {
        return new PromptReadyLocator(id, id, "name", id, "input", "LoginForm", "FORM",
                true, true, 1, 1, 0.9d, LocatorEvidenceType.CONFIRMED_LOCATOR, List.of("fixture"));
    }
}
