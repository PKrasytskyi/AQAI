package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement;
import ua.demo.agentlab.ui.discovery.spa.binding.ActionSequenceBinder;
import ua.demo.agentlab.ui.discovery.spa.binding.BehaviorExecutabilityGate;
import ua.demo.agentlab.ui.discovery.spa.binding.BehaviorSourceStateResolver;
import ua.demo.agentlab.ui.discovery.spa.binding.BehaviorTargetStateResolver;
import ua.demo.agentlab.ui.discovery.spa.binding.PostconditionBindingService;
import ua.demo.agentlab.ui.discovery.spa.binding.ScenarioDataBindingService;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorAssertion;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorStep;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;

import java.util.List;
import java.util.Map;

public class BehaviorBindingStageTest {

    @Test
    public void missingDataAndMissingUiEvidenceRemainDifferentFailures() {
        var data = new ScenarioDataBindingService().resolve(Map.of("query", "${UNSET_AGENTLAB_TEST_VALUE}"));
        Assert.assertTrue(data.values().isEmpty());
        Assert.assertTrue(data.reviewReasons().get(0).startsWith("Missing data value"));

        StructuredBehaviorContract contract = new StructuredBehaviorContract("REQ-1", "SEARCH",
                List.of("Click search"), List.of(), Map.of(), "", true, List.of());
        var decision = new BehaviorExecutabilityGate().evaluate(contract, List.<BoundSpaBehaviorStep>of(), List.of(), List.of());
        Assert.assertFalse(decision.executable());
        Assert.assertTrue(decision.reviewReasons().contains("No confirmed executable action binding was produced."));
    }

    @Test
    public void actionAndPostconditionStagesPreserveOrderAndFailureIdentity() {
        List<String> review = new java.util.ArrayList<>();
        List<BoundSpaBehaviorStep> steps = new ActionSequenceBinder().bind(
                List.of("type username", "click login"),
                action -> action.startsWith("type")
                        ? ActionSequenceBinder.StepResolution.bound(
                        new BoundSpaBehaviorStep("TYPE", "type-user", "username", "username", ""))
                        : ActionSequenceBinder.StepResolution.unbound("login locator is not confirmed"),
                review);
        Assert.assertEquals(steps.size(), 1);
        Assert.assertEquals(review, List.of("login locator is not confirmed"));

        StructuredAssertionRequirement assertion = new StructuredAssertionRequirement(
                "ELEMENT_VISIBLE", "dashboardHeading", "Dashboard",
                new SourceReference("requirements.md", 10, 10, "heading"));
        List<BoundSpaBehaviorAssertion> assertions = new PostconditionBindingService().bind(
                List.of(assertion), value -> new BoundSpaBehaviorAssertion(value, "", false,
                        "dashboard heading locator is not confirmed"), review);
        Assert.assertFalse(assertions.get(0).verifiable());
        Assert.assertTrue(review.contains("dashboard heading locator is not confirmed"));
    }

    @Test
    public void targetStateRequiresExplicitRouteOrUnambiguousCapability() {
        StructuredBehaviorContract contract = new StructuredBehaviorContract("REQ-2", "AUTHENTICATED_AREA",
                List.of(), List.of(), Map.of(), "targetRoute: /dashboard/index; pageCapability: AUTHENTICATED_AREA",
                true, List.of());
        SpaPageInventory login = new SpaPageInventory("login", "LoginPage", "/auth/login", "AUTHENTICATION",
                "login-fp", null, List.of(), List.of());
        SpaPageInventory dashboard = new SpaPageInventory("dashboard", "DashboardPage", "/dashboard/index",
                "AUTHENTICATED_AREA", "dashboard-fp", null, List.of(), List.of());
        var resolved = new BehaviorTargetStateResolver(new BehaviorSourceStateResolver())
                .resolve(contract, List.of(login, dashboard), null);
        Assert.assertTrue(resolved.isPresent());
        Assert.assertEquals(resolved.orElseThrow().pageId(), "dashboard");
    }
}
