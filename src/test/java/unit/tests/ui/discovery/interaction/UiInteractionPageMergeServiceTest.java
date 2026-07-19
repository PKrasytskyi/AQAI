package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPageMergeService;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage;

import java.util.List;

public class UiInteractionPageMergeServiceTest {

    @Test
    public void preservesStableDiscoveryEvidenceWhenLiveTargetUsesSameRoute() {
        CandidateLocatorEvidence stable = locator("base-locator", 0.89d, true,
                LocatorEvidenceType.CONFIRMED_LOCATOR);
        CandidateLocatorEvidence live = locator("live-locator", 0.66d, false,
                LocatorEvidenceType.CANDIDATE_LOCATOR);
        UiInteractionPage result = new UiInteractionPageMergeService().merge(page("base", stable), page("live", live));

        CandidateLocatorEvidence selected = result.components().get(0).locators().get(0);
        Assert.assertEquals(selected.locatorId(), "base-locator");
        Assert.assertTrue(selected.stableAcrossRuns());
        Assert.assertEquals(selected.observedEvidenceType(), LocatorEvidenceType.CONFIRMED_LOCATOR);
    }

    private UiInteractionPage page(String trace, CandidateLocatorEvidence locator) {
        SemanticComponentInventory component = new SemanticComponentInventory("dashboard:component:user-menu",
                "UserMenuComponent", ComponentType.USER_MENU, "css", locator.value(), "",
                List.of("dashboard:element:user-menu-trigger"), 0.90d, List.of(locator), List.of(), List.of(),
                List.of(trace));
        return new UiInteractionPage("dashboard", "DashboardPage", "/dashboard", "AUTHENTICATED_AREA",
                trace + "-fingerprint", metadata(), List.of(component), List.of(trace));
    }

    private CandidateLocatorEvidence locator(String id, double score, boolean stable, LocatorEvidenceType type) {
        return new CandidateLocatorEvidence(id, "dashboard:component:user-menu",
                "dashboard:element:user-menu-trigger", "css", "span.oxd-userdropdown-tab", score, true,
                1, 1, true, true, stable, type, SpaEvidenceStatus.CANDIDATE, List.of());
    }

    private KnowledgeRunMetadata metadata() {
        return new KnowledgeRunMetadata("run", "app", "base", "requirements", "session", "ui-knowledge-v2",
                "2026-07-18T00:00:00Z", "test", 1.0d);
    }
}
