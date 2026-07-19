package unit.tests.ai.ui.prompt.scope;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.assertions.model.AssertionSource;
import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.ai.ui.prompt.scope.*;
import ua.demo.agentlab.ui.discovery.catalog.*;

import java.util.List;

public class ConfirmedUiCatalogPomScopeProjectorTest {

    @Test
    public void projectsOnlyConfirmedPrimaryLocatorAndTypedAction() {
        ConfirmedCatalogLocator primary = locator("loc-primary", "id", "username", true, true, true, true);
        ConfirmedCatalogLocator standby = locator("loc-standby", "css", "input[name='username']", true, true, true, true);
        ConfirmedCatalogAction action = new ConfirmedCatalogAction("action-key", "TYPE", "usernameInput",
                "loc-primary", "loc-standby", 0.92d, List.of(), List.of(), List.of("REQ-1"));
        ConfirmedUiCatalog catalog = new ConfirmedUiCatalog(ConfirmedUiCatalog.SCHEMA_VERSION, "run", true,
                List.of(new ConfirmedCatalogPage("AUTHENTICATION", "login", "LoginPage", "/login", "state",
                        List.of(new ConfirmedCatalogComponent("form", "LoginForm", "FORM", List.of(action),
                                List.of(primary), List.of(standby))), List.of(), List.of())), List.of());
        PromptReadyPomScope result = new ConfirmedUiCatalogPomScopeProjector()
                .project(catalog, "LoginPage");

        Assert.assertEquals(result.allowedLocators().size(), 1);
        Assert.assertEquals(result.allowedLocators().get(0).id(), "usernameInput");
        Assert.assertEquals(result.ownedActions(), List.of("enterUsernameInput(String usernameInput)"));
        Assert.assertTrue(result.allowedLocators().get(0).sourceTrace().contains("catalog-locator-id:loc-primary"));
    }

    @Test
    public void returnsExplicitGapWhenCatalogDoesNotOwnRequestedPage() {
        ConfirmedUiCatalog catalog = new ConfirmedUiCatalog(ConfirmedUiCatalog.SCHEMA_VERSION, "run", true,
                List.of(), List.of());

        PromptReadyPomScope result = new ConfirmedUiCatalogPomScopeProjector()
                .project(catalog, "OtherPage");

        Assert.assertTrue(result.allowedLocators().isEmpty());
        Assert.assertTrue(result.ownedActions().isEmpty());
        Assert.assertTrue(result.coverageGaps().stream().anyMatch(value -> value.contains("no page-owned evidence")));
    }

    @Test
    public void projectsLoginCatalogIntoExactMethodsAndExecutableAssertions() {
        ConfirmedCatalogLocator username = locator("username-locator", "name", "username", true, true, true, true);
        ConfirmedCatalogLocator password = new ConfirmedCatalogLocator("password-locator", "password", "name",
                "password", 0.93d, "CONFIRMED_LOCATOR", true, true, true, true, List.of());
        ConfirmedCatalogLocator login = new ConfirmedCatalogLocator("login-locator", "login", "css",
                "button[type='submit']", 0.92d, "CONFIRMED_LOCATOR", true, true, true, true, List.of());
        List<ConfirmedCatalogAction> actions = List.of(
                action("TYPE", "username", "username-locator"),
                action("TYPE", "password", "password-locator"),
                action("SUBMIT_FORM", "login", "login-locator")
        );
        List<AssertionContract> assertions = List.of(
                assertion(AssertionType.URL_CONTAINS, "/auth/login"),
                assertion(AssertionType.ELEMENT_VISIBLE, "Username input is visible and enabled"),
                assertion(AssertionType.ELEMENT_VISIBLE, "Password input is visible and enabled"),
                assertion(AssertionType.ELEMENT_VISIBLE, "Login button is visible and enabled"),
                assertion(AssertionType.AUTHENTICATED_AREA_ABSENT,
                        "Authenticated area is no longer accessible after logout")
        );
        ConfirmedUiCatalog catalog = new ConfirmedUiCatalog(ConfirmedUiCatalog.SCHEMA_VERSION, "run", true,
                List.of(new ConfirmedCatalogPage("AUTHENTICATION", "login", "LoginPage", "/auth/login", "state",
                        List.of(new ConfirmedCatalogComponent("form", "LoginForm", "FORM", actions,
                                List.of(username, password, login), List.of())), assertions, List.of())), List.of());

        PromptReadyPomScope result = new ConfirmedUiCatalogPomScopeProjector().project(catalog, "LoginPage");

        Assert.assertEquals(result.ownedActions(), List.of(
                "clickLoginButton()", "enterPassword(String password)", "enterUsername(String username)"));
        Assert.assertTrue(result.ownedAssertions().stream().anyMatch(value ->
                value.type().equals("ELEMENT_VISIBLE") && value.targetLocatorId().equals("username")));
        Assert.assertTrue(result.ownedAssertions().stream().anyMatch(value ->
                value.type().equals("ELEMENT_VISIBLE") && value.targetLocatorId().equals("password")));
        Assert.assertTrue(result.ownedAssertions().stream().anyMatch(value ->
                value.type().equals("ELEMENT_VISIBLE") && value.targetLocatorId().equals("login")));
        Assert.assertEquals(result.ownedAssertions().stream()
                .filter(value -> value.type().equals("URL_CONTAINS") && value.expectedValue().equals("/auth/login"))
                .count(), 1L);
        Assert.assertTrue(result.coverageGaps().isEmpty());
    }

    private ConfirmedCatalogAction action(String intent, String target, String locatorId) {
        return new ConfirmedCatalogAction(intent + "-" + target, intent, target, locatorId, "", 0.9d,
                List.of(), List.of(), List.of("REQ-1"));
    }

    private AssertionContract assertion(AssertionType type, String expected) {
        return new AssertionContract("REQ-1", "TC-1", type, expected, "LoginPage", "/auth/login",
                "fixture.md [L1]", 1.0d, AssertionSource.REQUIREMENT);
    }

    private ConfirmedCatalogLocator locator(String id, String strategy, String value, boolean sameOrigin,
                                             boolean browserVerified, boolean stable, boolean unique) {
        return new ConfirmedCatalogLocator(id, "username", strategy, value, 0.9d, "CONFIRMED_LOCATOR",
                sameOrigin, browserVerified, stable, unique, List.of());
    }
}
