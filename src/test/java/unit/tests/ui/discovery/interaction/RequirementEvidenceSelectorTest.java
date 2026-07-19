package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.interaction.identity.LocatorEvidenceId;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticActionKey;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticElementKey;
import ua.demo.agentlab.ui.discovery.interaction.model.*;
import ua.demo.agentlab.ui.discovery.interaction.scope.RequirementEvidenceSelector;

import java.util.List;
import java.util.Map;

public class RequirementEvidenceSelectorTest {

    @Test
    public void selectsOnlyTypedActionOnConfirmedRequirementRoute() {
        InteractionCandidate dashboardLogout = candidate("dashboard", "/dashboard", "menu", "logout", SemanticAction.LOGOUT);
        InteractionCandidate loginSubmit = candidate("login", "/login", "form", "login-button", SemanticAction.SUBMIT_FORM);
        StructuredBehaviorContract requirement = new StructuredBehaviorContract(
                "REQ-4", "LOGOUT", List.of("Open user menu", "Logout"), List.of(), Map.of(),
                "sourceRoute: /dashboard; component: menu", true, List.of());

        var selection = new RequirementEvidenceSelector().select(List.of(requirement),
                List.of(dashboardLogout, loginSubmit));

        Assert.assertEquals(selection.selected().size(), 1);
        Assert.assertEquals(selection.selected().get(0).candidate().action(), SemanticAction.LOGOUT);
        Assert.assertEquals(selection.selected().get(0).requirementIds(), java.util.Set.of("REQ-4"));
        Assert.assertTrue(selection.unresolvedRequirements().isEmpty());
    }

    private InteractionCandidate candidate(String page, String route, String component, String role, SemanticAction action) {
        SemanticElementKey element = SemanticElementKey.of(page, "state", component, role);
        return new InteractionCandidate(element, new SemanticActionKey(element, action.name()),
                LocatorEvidenceId.of(element, "css", "." + role), action.name().toLowerCase() + "-" + role,
                role + "-css", page, route, component, action, "css", "." + role,
                true, true, true, true, true, false, 1, 1, 0.8d, 0.9d, List.of(), List.of("test"),
                new ScoreBreakdown("", Map.of(), 0.0d));
    }
}
