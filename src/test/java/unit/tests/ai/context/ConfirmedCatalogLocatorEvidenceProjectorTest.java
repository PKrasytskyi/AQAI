package unit.tests.ai.context;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.ConfirmedCatalogLocatorEvidenceProjector;
import ua.demo.agentlab.ui.discovery.catalog.*;

import java.util.List;

public class ConfirmedCatalogLocatorEvidenceProjectorTest {

    @Test
    public void projectsOnlyEligiblePrimaryLocators() {
        ConfirmedCatalogLocator primary = locator("primary", true, true, true, true);
        ConfirmedCatalogLocator unsafe = locator("unsafe", false, true, true, true);
        ConfirmedCatalogComponent component = new ConfirmedCatalogComponent(
                "login-form", "LoginForm", "FORM",
                List.of(new ConfirmedCatalogAction("type-user", "TYPE", "usernameInput",
                        "primary", "", 0.9d, List.of(), List.of(), List.of("REQ-1"))),
                List.of(primary, unsafe), List.of());
        ConfirmedUiCatalog catalog = new ConfirmedUiCatalog(ConfirmedUiCatalog.SCHEMA_VERSION, "run", true,
                List.of(new ConfirmedCatalogPage("AUTHENTICATION", "login", "LoginPage", "/login", "state",
                        List.of(component), List.of(), List.of())), List.of());

        var result = new ConfirmedCatalogLocatorEvidenceProjector().project(catalog);

        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).fieldHint(), "usernameInput");
        Assert.assertTrue(result.get(0).sourceTrace().contains("confirmed-catalog-primary"));
    }

    private ConfirmedCatalogLocator locator(
            String id, boolean sameOrigin, boolean browserVerified, boolean stable, boolean unique
    ) {
        return new ConfirmedCatalogLocator(id, "username", "name", "username", 0.9d,
                "CONFIRMED_LOCATOR", sameOrigin, browserVerified, stable, unique, List.of("requirement-id:REQ-1"));
    }
}
