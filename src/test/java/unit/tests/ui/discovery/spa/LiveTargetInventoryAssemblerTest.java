package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.evidence.model.DiscoveredPageEvidence;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredField;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredForm;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredInteractiveElement;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;
import ua.demo.agentlab.ui.discovery.selenium.model.RawPageSnapshot;
import ua.demo.agentlab.ui.discovery.spa.LiveTargetInventoryAssembler;
import ua.demo.agentlab.ui.discovery.spa.SourceStateBindingService;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTargetPageSnapshot;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage;

import java.util.List;
import java.util.Map;

public class LiveTargetInventoryAssemblerTest {

    @Test
    public void mergesRenderedTargetStateAsDistinctCurrentRunPage() {
        UiInteractionPage source = new UiInteractionPage("recruitment", "RecruitmentPage",
                "/recruitment/viewCandidates", "MODULE_NAVIGATION", "source-fp", metadata(), List.of(), List.of());
        DiscoveredPageSnapshot target = targetSnapshot();
        LiveTargetPageSnapshot liveTarget = new LiveTargetPageSnapshot("recruitment", source.route(), "openVacancies",
                target.pageId(), "/recruitment/viewJobVacancy", List.of("REQ-002"), target);

        UiInteractionInventory result = new LiveTargetInventoryAssembler().merge(profile(),
                new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                        List.of(source), List.of("base")), List.of(liveTarget), metadata(), config());

        Assert.assertEquals(result.pages().size(), 2);
        UiInteractionPage vacancies = result.pages().stream()
                .filter(page -> page.route().equals("/recruitment/viewJobVacancy"))
                .findFirst().orElseThrow();
        Assert.assertEquals(vacancies.pageId(), "web-index-php-recruitment-viewjobvacancy");
        Assert.assertTrue(vacancies.capability().contains("FILTER"),
                vacancies.capability() + " components=" + vacancies.components());
        Assert.assertTrue(vacancies.capability().contains("RECORD_LIST"), vacancies.capability());
        Assert.assertTrue(result.sourceTrace().contains("interaction-inventory:live-target-merge"));
    }

    @Test
    public void rebindsDependentFilterRequirementToLiveMappedTargetState() {
        UiInteractionPage source = new UiInteractionPage("recruitment", "RecruitmentPage",
                "/recruitment/viewCandidates", "MODULE_NAVIGATION", "source-fp", metadata(), List.of(), List.of());
        DiscoveredPageSnapshot target = targetSnapshot();
        LiveTargetPageSnapshot liveTarget = new LiveTargetPageSnapshot("recruitment", source.route(), "openVacancies",
                target.pageId(), "/recruitment/viewJobVacancy", List.of("REQ-NAV"), target);
        UiInteractionInventory inventory = new LiveTargetInventoryAssembler().merge(profile(),
                new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                        List.of(source), List.of("base")), List.of(liveTarget), metadata(), config());
        StructuredBehaviorContract filter = new StructuredBehaviorContract(
                "REQ-FILTER", "FILTER", List.of("Inspect vacancy filters"), List.of(), Map.of(),
                "targetPage: Vacancies; componentCapability: FILTER", true, List.of());

        var bindings = new SourceStateBindingService().bind(profile(), List.of(filter), inventory, config());

        Assert.assertEquals(bindings.bindings().size(), 1);
        Assert.assertEquals(bindings.bindings().get(0).sourcePageId(),
                "web-index-php-recruitment-viewjobvacancy");
        Assert.assertEquals(bindings.bindings().get(0).sourceRoute(), "/recruitment/viewJobVacancy");
        Assert.assertFalse(bindings.bindings().get(0).componentIds().isEmpty());
    }

    @Test
    public void mapsCustomSpaFiltersAndDivResultsAsRecordList() {
        UiInteractionPage source = new UiInteractionPage("recruitment", "RecruitmentPage",
                "/recruitment/viewCandidates", "MODULE_NAVIGATION", "source-fp", metadata(), List.of(), List.of());
        DiscoveredPageSnapshot target = customControlTargetSnapshot();
        LiveTargetPageSnapshot liveTarget = new LiveTargetPageSnapshot("recruitment", source.route(), "openVacancies",
                target.pageId(), "/recruitment/viewJobVacancy", List.of("REQ-FILTER"), target);

        UiInteractionInventory result = new LiveTargetInventoryAssembler().merge(profile(),
                new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                        List.of(source), List.of("base")), List.of(liveTarget), metadata(), config());

        UiInteractionPage vacancies = result.pages().stream()
                .filter(page -> page.route().equals("/recruitment/viewJobVacancy"))
                .findFirst().orElseThrow();
        Assert.assertTrue(vacancies.capability().contains("FILTER"),
                vacancies.capability() + " components=" + vacancies.components());
        Assert.assertTrue(vacancies.capability().contains("RECORD_LIST"), vacancies.capability());
        Assert.assertTrue(vacancies.components().stream().anyMatch(component -> component.type() == ComponentType.FILTER_PANEL));
        Assert.assertTrue(vacancies.components().stream().anyMatch(component -> component.type() == ComponentType.RESULTS_COLLECTION));
    }

    @Test
    public void preservesUserMenuTriggerAndLogoutFromLiveOverlaySnapshot() {
        UiInteractionPage dashboard = new UiInteractionPage("dashboard", "DashboardPage", "/dashboard/index",
                "AUTHENTICATED_AREA", "dashboard-fp", metadata(), List.of(), List.of());
        DiscoveredPageSnapshot target = dashboardMenuSnapshot();
        LiveTargetPageSnapshot liveTarget = new LiveTargetPageSnapshot("dashboard", dashboard.route(),
                "openUserMenu", target.pageId(), dashboard.route(), List.of("REQ-LOGOUT"), target);

        UiInteractionInventory result = new LiveTargetInventoryAssembler().merge(profile(),
                new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                        List.of(dashboard), List.of("base")), List.of(liveTarget), metadata(), config());

        UiInteractionPage mapped = result.pages().stream()
                .filter(page -> page.route().equals("/dashboard/index"))
                .findFirst().orElseThrow();
        var userMenu = mapped.components().stream()
                .filter(component -> component.type() == ComponentType.USER_MENU)
                .findFirst().orElseThrow();
        Assert.assertTrue(userMenu.elementIds().stream().anyMatch(id -> id.contains("user-menu-trigger")));
        Assert.assertTrue(userMenu.elementIds().stream().anyMatch(id -> id.contains("logout")), userMenu.toString());
        Assert.assertTrue(userMenu.actions().stream().anyMatch(action -> action.intent().equals("LOGOUT")));
    }

    private DiscoveredPageSnapshot targetSnapshot() {
        List<RawElement> elements = List.of(
                element("jobTitle", "select", "", "Job Title", "jobTitle", ""),
                element("status", "select", "", "Status", "status", ""),
                element("search", "button", "submit", "Search", "", "button"),
                element("results", "div", "", "Vacancies Results", "", "table")
        );
        RawPageSnapshot raw = new RawPageSnapshot("https://example.test/recruitment/viewJobVacancy",
                "/recruitment/viewJobVacancy", "Vacancies", "", "", "Vacancies Job Title Status Search Results",
                "", "", "", List.of(), Map.of(), Map.of(), List.of(), List.of(), List.of());
        DiscoveredForm filter = new DiscoveredForm("vacancy-filter", "", "", List.of(
                new DiscoveredField("select", "jobTitle", "", "Job Title", false, "", null),
                new DiscoveredField("select", "status", "", "Status", false, "", null)),
                List.of(new DiscoveredInteractiveElement("submit", "Search", "button", "", "", "",
                        true, true, null)));
        return new DiscoveredPageSnapshot("web-index-php-recruitment-viewjobvacancy",
                "https://example.test/recruitment/viewJobVacancy", "Vacancies", List.of("Vacancies"),
                List.of(), List.of(), List.of(filter), false, true, List.of("form", "authenticated-area"), List.of(),
                "target-fp", new DiscoveredPageEvidence("", ""), raw, elements);
    }

    private DiscoveredPageSnapshot customControlTargetSnapshot() {
        String jobTitleXpath = "(//label[normalize-space()='Job Title']/ancestor::*[.//*[@tabindex]][1]//*[@tabindex][1])";
        String statusXpath = "(//label[normalize-space()='Status']/ancestor::*[.//*[@tabindex]][1]//*[@tabindex][1])";
        Map<String, String> jobAttributes = Map.of(
                "tabindex", "0", "agentlab.container.key", "tag=form", "agentlab.container.tag", "form",
                "agentlab.field.kind", "custom-select", "agentlab.field.label", "Job Title",
                "agentlab.field.locator.xpath", jobTitleXpath);
        Map<String, String> statusAttributes = Map.of(
                "tabindex", "0", "agentlab.container.key", "tag=form", "agentlab.container.tag", "form",
                "agentlab.field.kind", "custom-select", "agentlab.field.label", "Status",
                "agentlab.field.locator.xpath", statusXpath);
        Map<String, String> submitAttributes = Map.of(
                "type", "submit", "agentlab.container.key", "tag=form", "agentlab.container.tag", "form");
        Map<String, String> tableAttributes = Map.of(
                "role", "table", "agentlab.container.key", "role=table:tag=div",
                "agentlab.container.tag", "div", "agentlab.container.role", "table");
        List<RawElement> elements = List.of(
                raw("jobTitle", "div", "", "-- Select --", "", "", "select-input", jobAttributes,
                        Map.of("xpath::" + jobTitleXpath, 1)),
                raw("status", "div", "", "-- Select --", "", "", "select-input", statusAttributes,
                        Map.of("xpath::" + statusXpath, 1)),
                raw("search", "button", "submit", "Search", "", "", "search-button", submitAttributes,
                        Map.of("css::button[type='submit']", 1)),
                raw("results", "div", "", "7 Records Found", "", "table", "results-grid", tableAttributes,
                        Map.of("css::div[role='table']", 1))
        );
        RawPageSnapshot raw = new RawPageSnapshot("https://example.test/recruitment/viewJobVacancy",
                "/recruitment/viewJobVacancy", "Vacancies", "", "", "Vacancies Job Title Status Search Records",
                "", "", "", List.of(), Map.of(), Map.of(), List.of(), List.of(), List.of());
        DiscoveredForm filter = new DiscoveredForm("vacancy-filter", "", "", List.of(),
                List.of(new DiscoveredInteractiveElement("submit", "Search", "button", "", "", "",
                        true, true, null)));
        return new DiscoveredPageSnapshot("web-index-php-recruitment-viewjobvacancy",
                "https://example.test/recruitment/viewJobVacancy", "Vacancies", List.of("Vacancies"),
                List.of(), List.of(), List.of(filter), false, true, List.of("form", "authenticated-area"), List.of(),
                "target-fp", new DiscoveredPageEvidence("", ""), raw, elements);
    }

    private DiscoveredPageSnapshot dashboardMenuSnapshot() {
        RawElement trigger = raw("menu-trigger", "span", "", "John Smith", "", "button",
                "oxd-userdropdown-tab", Map.of("role", "button"),
                Map.of("css::span.oxd-userdropdown-tab", 1));
        RawElement logout = new RawElement("logout", "a", "", "Logout", "", "", "", "", "menuitem",
                "/auth/logout", "", "oxd-userdropdown-link", true, true, false,
                Map.of("href", "/auth/logout", "role", "menuitem", "agentlab.container.role", "menu"),
                Map.of("css::a[href='/auth/logout']", 1, "xpath:://a[normalize-space()='Logout']", 1),
                Map.of("css::a[href='/auth/logout']", 1, "xpath:://a[normalize-space()='Logout']", 1),
                Map.of("css::a[href='/auth/logout']", "header", "xpath:://a[normalize-space()='Logout']", "header"));
        RawPageSnapshot raw = new RawPageSnapshot("https://example.test/dashboard/index", "/dashboard/index",
                "Dashboard", "", "", "Dashboard John Smith Logout", "", "", "", List.of(), Map.of(),
                Map.of(), List.of(), List.of(), List.of());
        return new DiscoveredPageSnapshot("dashboard", "https://example.test/dashboard/index", "Dashboard",
                List.of("Dashboard"), List.of(), List.of(), List.of(), false, true,
                List.of("authenticated-area", "logout"), List.of(), "dashboard-menu-fp",
                new DiscoveredPageEvidence("", ""), raw, List.of(trigger, logout));
    }

    private RawElement raw(String id, String tag, String type, String text, String name, String role,
                           String cssClass, Map<String, String> attributes, Map<String, Integer> counts) {
        return new RawElement(id, tag, type, text, "", name, "", "", role, "", "", cssClass,
                true, true, false, attributes, counts, counts, Map.of());
    }

    private RawElement element(String id, String tag, String type, String text, String name, String role) {
        String css = !name.isBlank() ? "[name='" + name + "']" : tag;
        return new RawElement(id, tag, type, text, "", name, "", "", role, "", "", "", true, true,
                false, Map.of(), Map.of(css, 1), Map.of(css, 1), Map.of(css, "page"));
    }

    private ProjectProfile profile() {
        return new ProjectProfile("test", "Test", "https://example.test", "/auth/login", "/auth/login", "",
                "/dashboard", "", "", "", "", "", "", "", new OutputProfile("pages", "tests"));
    }

    private KnowledgeRunMetadata metadata() {
        return new KnowledgeRunMetadata("run", "app", "base", "requirements", "session", "ui-knowledge-v2",
                "2026-07-16T00:00:00Z", "test", 1.0d);
    }

    private SpaInventoryConfig config() {
        return new SpaInventoryConfig(true, SpaDiscoveryMode.TARGETED, 30, true, 0.80d, 2, 2);
    }
}
