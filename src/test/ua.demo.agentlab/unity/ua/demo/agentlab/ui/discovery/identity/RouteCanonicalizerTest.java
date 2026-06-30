package ua.demo.agentlab.ui.discovery.identity;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RouteCanonicalizerTest {

    @Test
    public void stripsFrontControllerPrefixFromAbsoluteUrl() {
        String route = RouteCanonicalizer.canonicalize(
                "https://opensource-demo.orangehrmlive.com/web/index.php/auth/login?x=1"
        );

        Assert.assertEquals(route, "/auth/login");
    }

    @Test
    public void matchesProfileRouteAgainstDiscoveredFrontControllerUrl() {
        Assert.assertTrue(RouteCanonicalizer.routeEqualsOrSuffix(
                "/auth/login",
                "https://opensource-demo.orangehrmlive.com/web/index.php/auth/login"
        ));
    }
}
