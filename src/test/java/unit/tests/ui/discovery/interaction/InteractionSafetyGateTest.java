package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.interaction.identity.LocatorEvidenceId;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticActionKey;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticElementKey;
import ua.demo.agentlab.ui.discovery.interaction.model.InteractionCandidate;
import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;
import ua.demo.agentlab.ui.discovery.interaction.model.ScoreBreakdown;
import ua.demo.agentlab.ui.discovery.interaction.model.SemanticAction;
import ua.demo.agentlab.ui.discovery.interaction.selection.InteractionSafetyGate;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InteractionSafetyGateTest {

    @Test
    public void acceptsStableProductClassAndRejectsGeneratedHashClass() {
        InteractionSafetyGate gate = new InteractionSafetyGate();

        Assert.assertTrue(gate.evaluate(scoped("span.oxd-userdropdown-tab")).allowed());
        Assert.assertTrue(gate.evaluate(scoped("button.css-1abc23")).rejectionCodes()
                .contains("DYNAMIC_HASH_SELECTOR"));
    }

    private RequirementScopedInteraction scoped(String selector) {
        SemanticElementKey element = SemanticElementKey.of("dashboard", "base", "user-menu", "user-menu-trigger");
        SemanticActionKey action = new SemanticActionKey(element, "OPEN_MENU");
        InteractionCandidate candidate = new InteractionCandidate(element, action,
                LocatorEvidenceId.of(element, "css", selector), "open-menu", "menu-locator", "dashboard",
                "/dashboard", "user-menu", SemanticAction.OPEN_MENU, "css", selector, true, true, true,
                true, true, true, 1, 1, 0.80d, 0.90d, List.of(), List.of(),
                new ScoreBreakdown("test", Map.of(), 0.90d));
        return new RequirementScopedInteraction(candidate, Set.of("REQ-003"), 1.0d, 1.0d, 1.0d,
                candidate.intrinsicScore());
    }
}
