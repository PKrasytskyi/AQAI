package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.spa.ComponentActionDependencyResolver;
import ua.demo.agentlab.ui.discovery.spa.model.ComponentActionDependency;

import java.util.List;
import java.util.Map;

public class ComponentActionDependencyResolverTest {

    @Test
    public void acceptsLiveVerifiedDuplicateAsTheSameSemanticMenuOpener() {
        ComponentActionDependency dependency = new ComponentActionDependency(
                "dashboard", "user-menu",
                "dashboard:header:action:open-menu-user-menu-trigger",
                "dashboard:user-menu:action:logout",
                "menu must be open", 0.9d, List.of());

        boolean passed = new ComponentActionDependencyResolver().passed(
                dependency,
                Map.of("dashboard:header:action:open-menu-user-menu-trigger-2", true));

        Assert.assertTrue(passed);
    }

    @Test
    public void rejectsDifferentOrFailedPrerequisites() {
        ComponentActionDependency dependency = new ComponentActionDependency(
                "dashboard", "user-menu", "open-user-menu", "logout", "menu must be open", 0.9d, List.of());

        ComponentActionDependencyResolver resolver = new ComponentActionDependencyResolver();

        Assert.assertFalse(resolver.passed(dependency, Map.of("open-support-menu", true)));
        Assert.assertFalse(resolver.passed(dependency, Map.of("open-user-menu", false)));
    }
}
