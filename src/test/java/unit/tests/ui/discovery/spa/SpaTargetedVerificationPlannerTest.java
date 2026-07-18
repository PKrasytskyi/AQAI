package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.SpaTargetedVerificationPlanner;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;

import java.util.List;
import java.util.Map;

public class SpaTargetedVerificationPlannerTest {

    @Test
    public void verifiesOnlyFormEvidenceRequiredByEnterTextScenario() {
        var result = new SpaTargetedVerificationPlanner().verify(
                inventory(),
                new CanonicalTestCaseBundle("test", "LoginPage", List.of("LoginPage"), List.of(loginCase())),
                config()
        );

        Assert.assertEquals(result.locatorVerifications().size(), 1);
        Assert.assertTrue(result.locatorVerifications().get(0).verified());
        Assert.assertEquals(result.actionVerifications().size(), 1);
        Assert.assertTrue(result.actionVerifications().get(0).verified());
    }

    @Test
    public void excludesEvidenceWhenScenarioDoesNotOwnThePage() {
        CanonicalTestCase unrelated = new CanonicalTestCase(
                "REQ-2", "Dashboard search", List.of("REQ-2"), List.of(),
                List.of(new UiOperationIntent(UiOperationKind.SEARCH, "DashboardPage", null)), List.of(),
                List.of("DashboardPage"), null, "flow", "SEARCH", "DashboardPage", "DashboardPage",
                "/dashboard", "/dashboard", "", UiAssertionProfile.BASIC, List.of(), List.of(), List.of(), "test"
        );

        var result = new SpaTargetedVerificationPlanner().verify(
                inventory(), new CanonicalTestCaseBundle("test", "DashboardPage", List.of("DashboardPage"), List.of(unrelated)), config());

        Assert.assertTrue(result.locatorVerifications().isEmpty());
        Assert.assertTrue(result.actionVerifications().isEmpty());
    }

    @Test
    public void usesStructuredModuleNavigationScopeWhenCanonicalOperationsAreGeneric() {
        CandidateLocatorEvidence recruitment = new CandidateLocatorEvidence(
                "dashboard:navigation:recruitment", "dashboard:navigation", "recruitment", "css", "a[href='/recruitment']", 0.92d, true,
                1, 1, true, true, true, LocatorEvidenceType.CANDIDATE_LOCATOR, SpaEvidenceStatus.CANDIDATE, List.of());
        CandidateActionEvidence click = new CandidateActionEvidence(
                "dashboard:navigation:open-recruitment", "dashboard:navigation", "CLICK", "recruitment", 0.91d,
                List.of("dashboard:navigation:recruitment"), List.of("route=/dashboard"), List.of(), List.of(), SpaEvidenceStatus.CANDIDATE);
        SemanticComponentInventory navigation = new SemanticComponentInventory(
                "dashboard:navigation", "Navigation", ComponentType.NAVIGATION, "css", "nav", "", List.of("recruitment"), 0.9d,
                List.of(recruitment), List.of(click), List.of(), List.of("test"));
        SpaInventoryBundle dashboardInventory = new SpaInventoryBundle(SpaInventoryBundle.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                List.of(new SpaPageInventory("dashboard", "DashboardPage", "/dashboard", "AUTHENTICATED_AREA", "fp", metadata(),
                        List.of(navigation), List.of("test"))), List.of("test"));
        StructuredBehaviorContract contract = new StructuredBehaviorContract("REQ-001", "module-navigation", List.of("Open Recruitment module."),
                List.of(), Map.of(),
                "sourceRoute: /dashboard; targetPage: Recruitment", true, List.of());

        var result = new SpaTargetedVerificationPlanner().verify(dashboardInventory,
                new CanonicalTestCaseBundle("test", "", List.of(), List.of()), List.of(contract), config());

        Assert.assertEquals(result.locatorVerifications().size(), 1);
        Assert.assertTrue(result.locatorVerifications().get(0).verified());
        Assert.assertEquals(result.actionVerifications().size(), 1);
        Assert.assertTrue(result.actionVerifications().get(0).verified());
    }

    @Test
    public void selectsOnlyTheActualUserMenuOpenerForOpenMenuRequirements() {
        CandidateLocatorEvidence opener = locator(
                "dashboard:header:opener", "dashboard:header", "user-menu-trigger", "span.oxd-userdropdown-tab");
        CandidateLocatorEvidence about = locator(
                "dashboard:header:about", "dashboard:header", "about", "a.oxd-userdropdown-link[href='#']");
        CandidateActionEvidence openMenu = action(
                "dashboard:header:open-menu", "dashboard:header", "OPEN_MENU", "user-menu-trigger", opener.locatorId());
        CandidateActionEvidence falseOpenMenu = action(
                "dashboard:header:open-about", "dashboard:header", "OPEN_MENU", "about", about.locatorId());
        SemanticComponentInventory header = new SemanticComponentInventory(
                "dashboard:header", "Header", ComponentType.HEADER, "css", "header", "", List.of(), 0.9d,
                List.of(opener, about), List.of(openMenu, falseOpenMenu), List.of(), List.of("test"));
        SpaInventoryBundle dashboard = new SpaInventoryBundle(
                SpaInventoryBundle.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                List.of(new SpaPageInventory("dashboard", "DashboardPage", "/dashboard", "AUTHENTICATED_AREA",
                        "fp", metadata(), List.of(header), List.of("test"))), List.of("test"));
        CanonicalTestCase openMenuCase = new CanonicalTestCase(
                "REQ-MENU", "Open user menu", List.of("REQ-MENU"), List.of(),
                List.of(new UiOperationIntent(UiOperationKind.OPEN_MENU, "DashboardPage", null)), List.of(),
                List.of("DashboardPage"), null, "flow", "OPEN_MENU", "DashboardPage", "DashboardPage",
                "/dashboard", "/dashboard", "", UiAssertionProfile.BASIC, List.of(), List.of(), List.of(), "test");

        var result = new SpaTargetedVerificationPlanner().verify(
                dashboard,
                new CanonicalTestCaseBundle("test", "DashboardPage", List.of("DashboardPage"), List.of(openMenuCase)),
                config());

        Assert.assertEquals(result.actionVerifications().size(), 1);
        Assert.assertEquals(result.actionVerifications().get(0).actionId(), openMenu.actionId());
    }

    private SpaInventoryBundle inventory() {
        CandidateLocatorEvidence username = new CandidateLocatorEvidence(
                "login:form:username", "login:form", "username", "name", "username", 0.92d, true,
                1, 1, true, true, true, LocatorEvidenceType.CANDIDATE_LOCATOR, SpaEvidenceStatus.CANDIDATE, List.of()
        );
        CandidateActionEvidence type = new CandidateActionEvidence(
                "login:form:type-username", "login:form", "TYPE", "username", 0.90d,
                List.of("login:form:username"), List.of("route=/login"), List.of(), List.of(), SpaEvidenceStatus.CANDIDATE
        );
        SemanticComponentInventory form = new SemanticComponentInventory(
                "login:form", "LoginForm", ComponentType.FORM, "css", "form", "", List.of("username"), 0.9d,
                List.of(username), List.of(type), List.of(), List.of("test")
        );
        return new SpaInventoryBundle(
                SpaInventoryBundle.SCHEMA_VERSION, SpaDiscoveryMode.INVENTORY,
                List.of(new SpaPageInventory("login", "LoginPage", "/login", "AUTHENTICATION", "fp", metadata(),
                        List.of(form), List.of("test"))), List.of("test")
        );
    }

    private CanonicalTestCase loginCase() {
        return new CanonicalTestCase(
                "REQ-1", "Enter username", List.of("REQ-1"), List.of(),
                List.of(new UiOperationIntent(UiOperationKind.ENTER_TEXT, "LoginPage", null)), List.of(),
                List.of("LoginPage"), null, "flow", "ENTER_TEXT", "LoginPage", "LoginPage", "/login", "/login",
                "", UiAssertionProfile.BASIC, List.of(), List.of(), List.of(), "test"
        );
    }

    private SpaInventoryConfig config() {
        return new SpaInventoryConfig(true, SpaDiscoveryMode.TARGETED, 30, true, 0.80d, 2, 2);
    }

    private CandidateLocatorEvidence locator(String locatorId, String componentId, String elementId, String value) {
        return new CandidateLocatorEvidence(
                locatorId, componentId, elementId, "css", value, 0.92d, true,
                1, 1, true, true, true, LocatorEvidenceType.CANDIDATE_LOCATOR,
                SpaEvidenceStatus.CANDIDATE, List.of());
    }

    private CandidateActionEvidence action(
            String actionId,
            String componentId,
            String intent,
            String elementId,
            String locatorId
    ) {
        return new CandidateActionEvidence(
                actionId, componentId, intent, elementId, 0.91d, List.of(locatorId),
                List.of("route=/dashboard"), List.of(), List.of("test"), SpaEvidenceStatus.CANDIDATE);
    }

    private KnowledgeRunMetadata metadata() {
        return new KnowledgeRunMetadata("run", "app", "base", "requirements", "session", "ui-knowledge-v2",
                "2026-07-14T00:00:00Z", "test", 1.0d);
    }
}
