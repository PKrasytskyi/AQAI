package unit.tests.ai.context;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.LocatorEvidenceSelector;
import ua.demo.agentlab.ai.context.PromptPageScope;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.PageStateHints;

import java.util.List;
import java.util.Map;

public class LocatorEvidenceSelectorTest {

    @Test
    public void promotesDbCachedStableUserMenuTriggerToConfirmedLocatorEvidence() {
        PageModelEnrichmentRecord dashboardEnrichment = new PageModelEnrichmentRecord(
                "web-index-php-dashboard-index",
                "DashboardPage",
                "/dashboard/index",
                "dashboard",
                "Dashboard page",
                List.of("openUserMenu", "logout"),
                List.of("css=span.oxd-userdropdown-tab (stability=0.78, sameOrigin=true, element=User menu trigger, relevance=requirement, dependency=required-for-logout-menu-flow)"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("REQ-020"),
                Map.of(),
                Map.of(),
                0.78d,
                "db-cache"
        );

        List<ua.demo.agentlab.ai.context.PromptLocatorEvidence> locators = new LocatorEvidenceSelector()
                .select(contextWith(dashboardEnrichment), scopeFor(dashboardPage()));

        Assert.assertTrue(locators.stream().anyMatch(locator ->
                        locator.fieldHint().equals("userMenuTrigger")
                                && locator.value().equals("span.oxd-userdropdown-tab")
                                && locator.evidenceType() == LocatorEvidenceType.CONFIRMED_LOCATOR),
                "DB-cached stable user menu trigger must be allowed as confirmed POM evidence");
    }

    @Test
    public void keepsFreshEnrichmentStableLocatorsAsCandidateEvidence() {
        PageModelEnrichmentRecord dashboardEnrichment = new PageModelEnrichmentRecord(
                "web-index-php-dashboard-index",
                "DashboardPage",
                "/dashboard/index",
                "dashboard",
                "Dashboard page",
                List.of("openUserMenu"),
                List.of("css=span.oxd-userdropdown-tab (stability=0.78, sameOrigin=true, element=User menu trigger)"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("REQ-020"),
                Map.of(),
                Map.of(),
                0.78d,
                "openai"
        );

        List<ua.demo.agentlab.ai.context.PromptLocatorEvidence> locators = new LocatorEvidenceSelector()
                .select(contextWith(dashboardEnrichment), scopeFor(dashboardPage()));

        Assert.assertTrue(locators.stream().anyMatch(locator ->
                        locator.value().equals("span.oxd-userdropdown-tab")
                                && locator.evidenceType() == LocatorEvidenceType.CANDIDATE_LOCATOR),
                "Fresh enrichment must not bypass confirmed locator policy");
    }

    private AiContextPackage contextWith(PageModelEnrichmentRecord record) {
        return new AiContextPackage(
                "test",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(record),
                List.of(),
                List.of(),
                null
        );
    }

    private PromptPageScope scopeFor(MappedPage page) {
        return new PromptPageScope(page, page.pageName(), page.urlPattern(), true, List.of("LoginPage"), List.of("REQ-020"), List.of(), List.of());
    }

    private MappedPage dashboardPage() {
        return new MappedPage(
                "web-index-php-dashboard-index",
                "DashboardPage",
                "AUTHENTICATED_AREA",
                "https://opensource-demo.orangehrmlive.com/web/index.php/dashboard/index",
                "/dashboard/index",
                "OrangeHRM",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new PageStateHints(false, false, true, false, false, false),
                "",
                ""
        );
    }
}
