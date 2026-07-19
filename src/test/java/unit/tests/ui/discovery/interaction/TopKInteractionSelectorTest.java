package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.interaction.identity.*;
import ua.demo.agentlab.ui.discovery.interaction.model.*;
import ua.demo.agentlab.ui.discovery.interaction.selection.TopKInteractionSelector;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TopKInteractionSelectorTest {

    @Test
    public void rejectsUnsafeEvidenceAndPrefersDifferentSelectorFamilies() {
        List<RequirementScopedInteraction> source = List.of(
                interaction("id", "login", 0.95d, true),
                interaction("css", "button[type='submit']", 0.90d, true),
                interaction("xpath", "//button[normalize-space()='Login']", 0.85d, true),
                interaction("xpath", "/html/body/div/button", 0.99d, true));

        var result = new TopKInteractionSelector().select(source);

        Assert.assertEquals(result.selected().size(), 3);
        Assert.assertEquals(result.selected().get(0).candidate().strategy(), "id");
        Assert.assertEquals(result.selected().get(1).candidate().strategy(), "css");
        Assert.assertEquals(result.rejected().size(), 1);
        Assert.assertTrue(result.rejected().get(0).rejectionCodes().contains("ABSOLUTE_XPATH"));
    }

    private RequirementScopedInteraction interaction(String strategy, String value, double score, boolean sameOrigin) {
        SemanticElementKey element = SemanticElementKey.of("login", "state", "form", "login-button");
        InteractionCandidate candidate = new InteractionCandidate(
                element, new SemanticActionKey(element, "SUBMIT_FORM"), LocatorEvidenceId.of(element, strategy, value),
                "submit-login", "locator-" + strategy, "login", "/login", "form",
                SemanticAction.SUBMIT_FORM, strategy, value, true, true, sameOrigin,
                true, true, true, 1, 1, score, 0.95d, List.of(), List.of("test"),
                new ScoreBreakdown("interaction-score.v1", Map.of("score", score), score));
        return new RequirementScopedInteraction(candidate, Set.of("REQ-1"), 1.0d, 1.0d, 1.0d,
                new ScoreBreakdown("interaction-score.v1", Map.of("score", score), score));
    }
}
