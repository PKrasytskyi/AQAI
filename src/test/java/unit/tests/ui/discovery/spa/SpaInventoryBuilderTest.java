package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageEvidenceModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFormModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFlowModel;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryBuilder;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;

import java.util.List;
import java.util.Map;

public class SpaInventoryBuilderTest {

    @Test
    public void buildsCandidateOnlyInventoryForDiscoveredForm() {
        var inventory = new SpaInventoryBuilder().build(
                new PageModelBundle(List.of(loginPage())),
                MappedUiKnowledge.empty(),
                metadata(),
                new SpaInventoryConfig(true, SpaDiscoveryMode.INVENTORY, 30, true, 0.80d, 2, 2)
        );

        Assert.assertEquals(inventory.pages().size(), 1);
        Assert.assertFalse(inventory.pages().get(0).components().isEmpty());
        Assert.assertTrue(inventory.pages().get(0).components().stream()
                .flatMap(component -> component.locators().stream())
                .allMatch(locator -> locator.status() == SpaEvidenceStatus.CANDIDATE));
        Assert.assertTrue(inventory.sourceTrace().contains("candidate-only-no-pom-promotion"));
    }

    @Test
    public void disablesInventoryWithoutChangingMappedKnowledge() {
        var inventory = new SpaInventoryBuilder().build(
                new PageModelBundle(List.of(loginPage())),
                MappedUiKnowledge.empty(),
                metadata(),
                new SpaInventoryConfig(false, SpaDiscoveryMode.INVENTORY, 30, true, 0.80d, 2, 2)
        );

        Assert.assertTrue(inventory.pages().isEmpty());
        Assert.assertTrue(inventory.sourceTrace().contains("spa-inventory:disabled"));
    }

    @Test
    public void includesDiscoveryConfirmedProtectedTargetNamedByStructuredRequirement() {
        PageModel dashboard = new PageModel("dashboard", "https://example.test/dashboard", "/dashboard", "Dashboard", "Recruitment", "AUTHENTICATED_AREA",
                new PageEvidenceModel("", ""), List.of(), List.of(), List.of(),
                List.of(new PageFlowModel("dashboard:recruitment", "dashboard", "Recruitment", "CLICK", "recruitment", "/recruitment", true)));
        PageModel recruitment = new PageModel("recruitment", "https://example.test/recruitment", "/recruitment", "Recruitment", "Vacancies", "RECORD_LIST",
                new PageEvidenceModel("", ""), List.of(), List.of(), List.of(), List.of());
        CanonicalTestCase source = new CanonicalTestCase("REQ-001", "Open recruitment", List.of("REQ-001"), List.of(), List.of(), List.of(),
                List.of("DashboardPage"), null, "flow", "OPEN_PAGE", "DashboardPage", "DashboardPage", "/dashboard", "/dashboard", "",
                UiAssertionProfile.BASIC, List.of(), List.of(), List.of(), "test");
        StructuredBehaviorContract contract = new StructuredBehaviorContract("REQ-001", "module-navigation", List.of("Open the Recruitment module."),
                List.of(), Map.of(), "targetPage: discovery-confirmed Recruitment page", true, List.of());

        var inventory = new SpaInventoryBuilder().build(new PageModelBundle(List.of(dashboard, recruitment)), MappedUiKnowledge.empty(), metadata(),
                new SpaInventoryConfig(true, SpaDiscoveryMode.TARGETED, 30, true, 0.80d, 2, 2),
                new CanonicalTestCaseBundle("test", "DashboardPage", List.of("DashboardPage"), List.of(source)), List.of(contract));

        Assert.assertEquals(inventory.pages().stream().map(page -> page.pageId()).toList(), List.of("dashboard", "recruitment"));
    }

    @Test
    public void derivesRecordListCapabilityOnlyFromFilterAndCollectionComponents() {
        PageLocatorModel filterLocator = new PageLocatorModel("name", "status", 0.90d, "filter", true,
                1, 1, true, 1, 1, "filter");
        PageLocatorModel tableLocator = new PageLocatorModel("css", "table", 0.85d, "table", true,
                1, 1, true, 1, 1, "table");
        PageElementModel filter = new PageElementModel("filterPanel", "INPUT", "", "input", "text", "Filter criteria", "", "status", "", "", "", "",
                "", true, true, true, Map.of("name", "status"), List.of(filterLocator), filterLocator, List.of(), 0.90d);
        PageElementModel table = new PageElementModel("vacancyResults", "TABLE", "", "table", "", "Vacancies results", "", "", "", "", "", "",
                "", true, true, false, Map.of(), List.of(tableLocator), tableLocator, List.of(), 0.85d);
        PageModel vacancies = new PageModel("vacancies", "https://example.test/vacancies", "/vacancies", "Vacancies", "Vacancies", "detail",
                new PageEvidenceModel("", ""), List.of(filter, table), List.of(), List.of(), List.of());

        var inventory = new SpaInventoryBuilder().build(new PageModelBundle(List.of(vacancies)), MappedUiKnowledge.empty(), metadata(),
                new SpaInventoryConfig(true, SpaDiscoveryMode.INVENTORY, 30, true, 0.80d, 2, 2));

        Assert.assertTrue(inventory.pages().get(0).capability().contains("RECORD_LIST"));
        Assert.assertTrue(inventory.pages().get(0).capability().contains("FILTER"));
    }

    @Test
    public void doesNotTreatXPathSyntaxAsExternalOrigin() {
        PageLocatorModel vacancyLink = new PageLocatorModel("xpath", "//a[normalize-space()='Vacancies']", 0.76d,
                "top navigation", true, 1, 1, true, 1, 1, "navigation");
        PageElementModel vacancy = new PageElementModel("vacancies", "LINK", "", "link", "", "Vacancies", "#",
                "", "", "", "", "", "", true, true, false, Map.of(), List.of(vacancyLink), vacancyLink,
                List.of(), 0.76d);
        PageModel recruitment = new PageModel("recruitment", "https://example.test/recruitment", "/recruitment",
                "Recruitment", "Vacancies", "MODULE_NAVIGATION", new PageEvidenceModel("", ""),
                List.of(vacancy), List.of(), List.of(), List.of());

        var inventory = new SpaInventoryBuilder().build(new PageModelBundle(List.of(recruitment)),
                MappedUiKnowledge.empty(), metadata(),
                new SpaInventoryConfig(true, SpaDiscoveryMode.INVENTORY, 30, true, 0.80d, 2, 2));

        Assert.assertTrue(inventory.pages().get(0).components().stream()
                .flatMap(component -> component.locators().stream())
                .filter(locator -> locator.value().contains("Vacancies"))
                .allMatch(locator -> locator.sameOrigin()),
                "XPath // syntax must not be classified as an external URL");
        Assert.assertTrue(inventory.pages().get(0).components().stream()
                        .flatMap(component -> component.actions().stream())
                        .anyMatch(action -> action.targetElementId().equals("vacancies") && action.intent().equals("CLICK")),
                "Component inventory must retain element actions beyond the compact page-action summary");
    }

    private PageModel loginPage() {
        PageLocatorModel username = new PageLocatorModel("name", "username", 0.90d, "form field", true,
                2, 2, true, 1, 1, "form");
        PageLocatorModel submit = new PageLocatorModel("css", "button[type='submit']", 0.82d, "submit", true,
                2, 2, true, 1, 1, "form");
        PageElementModel usernameInput = new PageElementModel(
                "username", "INPUT", "", "input", "text", "", "", "username", "", "", "", "",
                "", true, true, true, Map.of("name", "username"), List.of(username), username, List.of(), 0.90d
        );
        PageElementModel loginButton = new PageElementModel(
                "loginButton", "BUTTON", "", "button", "", "Login", "", "", "", "", "button", "",
                "", true, true, false, Map.of(), List.of(submit), submit, List.of(), 0.82d
        );
        return new PageModel(
                "login", "https://example.test/auth/login", "/auth/login", "Login", "Login", "AUTHENTICATION",
                new PageEvidenceModel("", ""), List.of(usernameInput, loginButton),
                List.of(new PageFormModel("login", "login", "", List.of("username"), List.of("loginButton"))),
                List.of(), List.of()
        );
    }

    private KnowledgeRunMetadata metadata() {
        return new KnowledgeRunMetadata("run", "app", "base", "requirements", "session", "spa-page-inventory.v1",
                "2026-07-14T00:00:00Z", "test", 1.0d);
    }
}
