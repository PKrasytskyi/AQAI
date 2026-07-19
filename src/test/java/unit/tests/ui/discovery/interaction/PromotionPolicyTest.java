package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.interaction.identity.*;
import ua.demo.agentlab.ui.discovery.interaction.model.*;
import ua.demo.agentlab.ui.discovery.interaction.promotion.PromotionHistory;
import ua.demo.agentlab.ui.discovery.interaction.promotion.PromotionPolicy;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PromotionPolicyTest {

    @Test
    public void promotesOnlyStableFullyVerifiedEvidence() {
        LiveVerifiedInteraction interaction = interaction(true, 0.91d);
        var decision = new PromotionPolicy().decide(List.of(interaction), List.of()).get(0);
        Assert.assertEquals(decision.status(), InteractionEvidenceStatus.CONFIRMED);
        Assert.assertTrue(decision.primary());
    }

    @Test
    public void degradesConfirmedEvidenceFromRuntimeFailureHistory() {
        Assert.assertEquals(new PromotionPolicy().status(interaction(true, 0.91d),
                new PromotionHistory(8, 2)), InteractionEvidenceStatus.DEGRADED);
    }

    private LiveVerifiedInteraction interaction(boolean stable, double score) {
        SemanticElementKey element = SemanticElementKey.of("page", "/route", "component", "button");
        InteractionCandidate candidate = new InteractionCandidate(element, new SemanticActionKey(element, "CLICK"),
                LocatorEvidenceId.of(element, "id", "submit"), "click-submit", "submit-id",
                "page", "/route", "component",
                SemanticAction.CLICK, "id", "submit", true, true, true, true, true, stable,
                1, 1, 0.9d, 0.9d, List.of(), List.of(), new ScoreBreakdown("v1", Map.of(), score));
        RequirementScopedInteraction scoped = new RequirementScopedInteraction(candidate, Set.of("REQ-1"),
                1.0d, 1.0d, 1.0d, new ScoreBreakdown("v1", Map.of(), score));
        InteractionVerification verification = new InteractionVerification(true, true, true, true,
                1, 1, 0.95d, "passed", List.of());
        return new LiveVerifiedInteraction(scoped, verification, new ScoreBreakdown("v1", Map.of(), score));
    }
}
