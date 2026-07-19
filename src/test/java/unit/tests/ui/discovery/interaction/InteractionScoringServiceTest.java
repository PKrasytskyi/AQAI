package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.interaction.identity.LocatorEvidenceId;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticActionKey;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticElementKey;
import ua.demo.agentlab.ui.discovery.interaction.model.*;
import ua.demo.agentlab.ui.discovery.interaction.scoring.InteractionScoringService;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InteractionScoringServiceTest {

    @Test
    public void preservesVersionedBreakdownAcrossIntrinsicScopedAndLiveStages() {
        SemanticElementKey element = SemanticElementKey.of("login", "state-1", "form", "username");
        InteractionCandidate raw = new InteractionCandidate(
                element, new SemanticActionKey(element, "TYPE"), LocatorEvidenceId.of(element, "name", "username"),
                "type-username", "username-name", "login", "/login", "form", SemanticAction.TYPE, "name", "username", true, true, true,
                true, true, true, 1, 1, 0.90d, 0.96d, List.of(), List.of("test"),
                new ScoreBreakdown("", Map.of(), 0.0d));
        InteractionScoringService service = new InteractionScoringService();

        InteractionCandidate intrinsic = service.scoreIntrinsic(raw);
        RequirementScopedInteraction scoped = service.scoreScoped(intrinsic, Set.of("REQ-1"), 1.0d, 1.0d, 1.0d);
        LiveVerifiedInteraction verified = service.scoreLive(scoped,
                new InteractionVerification(true, true, true, true, 1, 1, 1.0d, "passed", List.of("live")));

        Assert.assertEquals(intrinsic.intrinsicScore().schemaVersion(), "interaction-score.v1");
        Assert.assertTrue(scoped.scopedScore().finalScore() >= intrinsic.intrinsicScore().finalScore());
        Assert.assertTrue(verified.finalScore().finalScore() >= scoped.scopedScore().finalScore());
        Assert.assertTrue(verified.finalScore().factors().containsKey("postcondition"));
    }
}
