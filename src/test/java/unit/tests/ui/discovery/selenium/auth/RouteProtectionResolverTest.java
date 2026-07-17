package unit.tests.ui.discovery.selenium.auth;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.selenium.auth.RouteProtectionResolver;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;

import java.util.List;

public class RouteProtectionResolverTest {

    private final RouteProtectionResolver resolver = new RouteProtectionResolver();
    private final ProjectProfile profile = new ProjectProfile(
            "the-internet", "The Internet", "https://the-internet.herokuapp.com",
            "/", "/login", "", "/secure", "", "", "", "", "", "", "",
            new OutputProfile("pages", "tests")
    );

    @Test
    public void keepsPublicPagesPublicWhenProfileAlsoContainsAuthenticatedRoute() {
        Assert.assertFalse(resolver.isDeclaredProtected(profile, page("checkboxes", "/checkboxes", "SELECTION"), List.of()));
        Assert.assertFalse(resolver.isDeclaredProtected(profile, page("inputs", "/inputs", "DATA_ENTRY"), List.of()));
        Assert.assertFalse(resolver.isDeclaredProtected(profile,
                page("dynamic-controls", "/dynamic_controls", "FORM|MODULE_NAVIGATION|AUTHENTICATED_AREA"), List.of()),
                "A mapper capability hypothesis must not override the explicit public route");
    }

    @Test
    public void recognizesExplicitProtectedRouteAndLiveLoginRedirect() {
        Assert.assertTrue(resolver.isDeclaredProtected(profile, page("secure", "/secure", "AUTHENTICATED_AREA"), List.of()));
        Assert.assertTrue(resolver.redirectedToLogin(profile, "/records", "https://the-internet.herokuapp.com/login"));
        Assert.assertFalse(resolver.redirectedToLogin(profile, "/checkboxes", "https://the-internet.herokuapp.com/checkboxes"));
    }

    private SpaPageInventory page(String id, String route, String capability) {
        return new SpaPageInventory(id, id + "Page", route, capability, "fp", null, List.of(), List.of());
    }
}
