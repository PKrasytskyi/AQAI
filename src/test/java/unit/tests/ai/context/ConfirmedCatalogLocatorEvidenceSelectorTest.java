package unit.tests.ai.context;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.*;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageStateHints;

import java.util.List;

public class ConfirmedCatalogLocatorEvidenceSelectorTest {

    @Test
    public void keepsOnlyConfirmedCatalogEvidenceOwnedByTargetPageAndRoute() {
        MappedPage dashboard = page("dashboard", "DashboardPage", "/dashboard/index");
        AiContextPackage context = context(dashboard, List.of(
                locator("userMenuTrigger", "dashboard", "/dashboard/index"),
                locator("foreignControl", "settings", "/settings")
        ));
        PromptPageScope scope = new PromptPageScope(dashboard, "DashboardPage", "/dashboard/index",
                true, List.of("LoginPage"), List.of("REQ-MENU"), List.of(), List.of());

        List<PromptLocatorEvidence> selected =
                new ConfirmedCatalogLocatorEvidenceSelector().select(context, scope);

        Assert.assertEquals(selected.size(), 1);
        Assert.assertEquals(selected.get(0).fieldHint(), "userMenuTrigger");
    }

    private AiContextPackage context(MappedPage page, List<PromptLocatorEvidence> locators) {
        return new AiContextPackage(
                "Generate POM", null, null, null, null, null, null, null,
                new MappedUiKnowledge(List.of(page), List.of(), List.of(), List.of(), List.of()),
                null, null, null, null, List.of(), List.of(), List.of(), List.of(), locators,
                PromptUiEvidence.empty("fixture"));
    }

    private PromptLocatorEvidence locator(String fieldHint, String pageId, String route) {
        return new PromptLocatorEvidence(fieldHint, fieldHint, "css", "#" + fieldHint, "button",
                fieldHint, "", true, 0.90d, pageId + ":component", "USER_MENU", 1, 1, true,
                LocatorEvidenceType.CONFIRMED_LOCATOR,
                List.of("confirmed-catalog-primary", "catalog-page-id:" + pageId,
                        "catalog-page-name:" + pageId + "Page", "catalog-route:" + route,
                        "requirement-id:REQ-MENU"));
    }

    private MappedPage page(String pageId, String pageName, String route) {
        return new MappedPage(pageId, pageName, "AUTHENTICATED_AREA", "https://example.test" + route,
                route, pageName, List.of(), List.of(), List.of(), List.of(), List.of(),
                new PageStateHints(false, false, true, false, false, false), "", "");
    }
}
