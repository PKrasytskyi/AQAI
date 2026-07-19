package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.spa.LiveTransitionDiscoveryService;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaStateGraph;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateTransition;

import java.util.List;

public class LiveTransitionDiscoveryServiceTest {

    @Test
    public void bindsTargetRouteOnlyFromBrowserObservedTransition() {
        SourceStateBinding source = new SourceStateBinding("REQ-1", "MODULE_NAVIGATION", "dashboard", "/dashboard",
                "Recruitment", List.of("navigation"), List.of("recruitment"), List.of("openRecruitment"), true, List.of());
        UiStateSnapshot before = state("state-dashboard", "/dashboard");
        UiStateSnapshot after = state("state-recruitment", "/recruitment/viewCandidates");
        UiStateTransition transition = new UiStateTransition("transition-1", before.stateId(), after.stateId(),
                "openRecruitment", "CLICK", "route changed", true, false, 0.90d, null, List.of());
        TargetedLocatorVerification locator = new TargetedLocatorVerification("dashboard", "/dashboard", "fp", "navigation",
                "recruitment", "recruitmentLink", "css", "a[href='/recruitment/viewCandidates']", 0.72d, true,
                "live confirmed", List.of("REQ-1"));
        TargetedActionVerification action = new TargetedActionVerification("dashboard", "/dashboard", "fp", "navigation",
                "openRecruitment", "CLICK", "recruitmentLink", 0.80d, true, "live confirmed", List.of("REQ-1"));
        SpaLiveTargetedVerificationResult live = new SpaLiveTargetedVerificationResult(
                SpaLiveTargetedVerificationResult.SCHEMA_VERSION, null, true, true, List.of(locator), List.of(action),
                new SpaStateGraph(SpaStateGraph.SCHEMA_VERSION, null, List.of(before, after), List.of(transition), List.of()),
                List.of());

        var result = new LiveTransitionDiscoveryService().discover(
                new SourceStateBindingBundle(SourceStateBindingBundle.SCHEMA_VERSION, null, List.of(source), List.of()), live);

        Assert.assertTrue(result.transitions().get(0).confirmed());
        Assert.assertEquals(result.transitions().get(0).targetRoute(), "/recruitment/viewCandidates");
        Assert.assertEquals(result.transitions().get(0).locatorId(), "recruitment");
    }

    private UiStateSnapshot state(String id, String route) {
        return new UiStateSnapshot(id, "dashboard", route, id, null, List.of(), List.of(), List.of(), List.of(),
                false, true, true, 0.95d, List.of());
    }
}
