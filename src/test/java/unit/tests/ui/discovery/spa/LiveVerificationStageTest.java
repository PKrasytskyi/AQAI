package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.spa.LiveVerificationResultAssembler;
import ua.demo.agentlab.ui.discovery.spa.StateTransitionCaptureService;
import ua.demo.agentlab.ui.discovery.spa.model.*;

import java.util.List;

public class LiveVerificationStageTest {

    @Test
    public void transitionAndRunPassAreAssembledFromVerifiedEvidence() {
        UiStateSnapshot before = state("before", "/login", false);
        UiStateSnapshot after = state("after", "/secure", true);
        TargetedActionVerification action = new TargetedActionVerification("login", "/login", "fp", "form",
                "submit", "SUBMIT_FORM", "loginButton", 0.9d, true, "passed", List.of("REQ-1"));
        UiStateTransition transition = new StateTransitionCaptureService().capture(before, after, action);
        Assert.assertTrue(transition.routeChanged());

        var locator = new TargetedLocatorVerification("login", "/login", "fp", "form", "username", "username",
                "id", "username", 0.9d, true, "passed", List.of("REQ-1"));
        var result = new LiveVerificationResultAssembler().success(null, List.of(locator), List.of(action),
                List.of(before, after), List.of(transition), List.of(), List.of("test"));
        Assert.assertTrue(result.passed());
        Assert.assertEquals(result.stateGraph().transitions().size(), 1);
    }

    private UiStateSnapshot state(String id, String route, boolean authenticated) {
        return new UiStateSnapshot(id, "page", route, id + "-fp", null, List.of(), List.of(), List.of(), List.of(),
                false, true, authenticated, 0.9d, List.of());
    }
}
