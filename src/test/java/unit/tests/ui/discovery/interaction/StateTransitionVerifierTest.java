package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.interaction.identity.LocatorEvidenceId;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticActionKey;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticElementKey;
import ua.demo.agentlab.ui.discovery.interaction.model.*;
import ua.demo.agentlab.ui.discovery.interaction.verification.StateTransitionVerifier;
import ua.demo.agentlab.ui.discovery.spa.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class StateTransitionVerifierTest {

    @Test
    public void confirmsSameRouteMenuTransitionBySemanticActionIdentity() {
        RequirementScopedInteraction scoped = scoped(SemanticAction.OPEN_MENU, "menu-action-2");
        UiStateTransition transition = new UiStateTransition("t1", "base", "menu-open", "menu-action-2",
                "OPEN_MENU", "menu visible", false, true, 0.92d, null, List.of());
        SpaLiveTargetedVerificationResult live = new SpaLiveTargetedVerificationResult(
                SpaLiveTargetedVerificationResult.SCHEMA_VERSION, null, true, true, List.of(), List.of(),
                new SpaStateGraph(SpaStateGraph.SCHEMA_VERSION, null, List.of(), List.of(transition), List.of()), List.of());

        var result = new StateTransitionVerifier().verify(scoped, live);

        Assert.assertTrue(result.passed());
        Assert.assertEquals(result.confidence(), 0.92d);
    }

    @Test
    public void rejectsTransitionActionWithoutObservableStateChange() {
        var result = new StateTransitionVerifier().verify(scoped(SemanticAction.LOGOUT, "logout-action"),
                SpaLiveTargetedVerificationResult.skipped(null, "none"));
        Assert.assertFalse(result.passed());
        Assert.assertTrue(result.reason().contains("missing"));
    }

    private RequirementScopedInteraction scoped(SemanticAction action, String sourceActionId) {
        SemanticElementKey element = SemanticElementKey.of("dashboard", "/dashboard", "header", "control");
        InteractionCandidate candidate = new InteractionCandidate(element, new SemanticActionKey(element, action.name()),
                LocatorEvidenceId.of(element, "css", "button"), sourceActionId, "control-css",
                "dashboard", "/dashboard", "header", action,
                "css", "button", true, true, true, true, true, true, 1, 1, 0.9d, 0.9d,
                List.of(), List.of("test"),
                new ScoreBreakdown("interaction-score.v1", Map.of(), 0.9d));
        return new RequirementScopedInteraction(candidate, Set.of("REQ-1"), 1.0d, 1.0d, 1.0d,
                new ScoreBreakdown("interaction-score.v1", Map.of(), 0.9d));
    }
}
