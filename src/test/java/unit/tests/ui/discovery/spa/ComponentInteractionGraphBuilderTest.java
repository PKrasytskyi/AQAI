package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.spa.ComponentInteractionGraphBuilder;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage;

import java.util.List;

public class ComponentInteractionGraphBuilderTest {

    @Test
    public void requiresUserMenuBeforeLogoutWhenBothActionsAreOnTheSameSpaPage() {
        SemanticComponentInventory userMenu = new SemanticComponentInventory(
                "dashboard:user-menu", "UserMenuComponent", ComponentType.USER_MENU, "css", "header", "",
                List.of("userMenu", "logout"), 0.9d, List.of(), List.of(
                action("open-user-menu", "OPEN_MENU", "userMenu"),
                action("logout", "LOGOUT", "logout")
        ), List.of(), List.of("test"));
        UiInteractionInventory inventory = new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                List.of(new UiInteractionPage("dashboard", "DashboardPage", "/dashboard", "AUTHENTICATED_AREA", "fp", metadata(),
                        List.of(userMenu), List.of("test"))), List.of("test"));

        var graph = new ComponentInteractionGraphBuilder().build(inventory);

        Assert.assertEquals(graph.dependencies().size(), 1);
        Assert.assertEquals(graph.dependencies().get(0).prerequisiteActionId(), "open-user-menu");
        Assert.assertEquals(graph.dependencies().get(0).dependentActionId(), "logout");
    }

    private CandidateActionEvidence action(String id, String intent, String element) {
        return new CandidateActionEvidence(id, "dashboard:user-menu", intent, element, 0.9d,
                List.of(element + "-locator"), List.of(), List.of(), List.of("test"), SpaEvidenceStatus.CANDIDATE);
    }

    private KnowledgeRunMetadata metadata() {
        return new KnowledgeRunMetadata("run", "app", "base", "requirements", "session", "ui-knowledge-v2",
                "2026-07-14T00:00:00Z", "test", 1.0d);
    }
}
