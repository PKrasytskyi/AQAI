package unit.tests.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.interaction.pipeline.CanonicalInteractionEvidenceAssembler;
import ua.demo.agentlab.ui.discovery.interaction.pipeline.CanonicalInteractionEvidenceBundle;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GoldenLoginDashboardInteractionSnapshotTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void canonicalInteractionTopologyMatchesGoldenLoginDashboardSnapshot() throws Exception {
        CanonicalInteractionEvidenceBundle bundle = new CanonicalInteractionEvidenceAssembler()
                .assemble(inventory(), live(), requirements());
        List<Map<String, String>> interactions = bundle.promotionDecisions().stream()
                .filter(item -> item.primary())
                .map(item -> {
                    var candidate = item.interaction().scopedInteraction().candidate();
                    Map<String, String> value = new LinkedHashMap<>();
                    value.put("pageId", candidate.pageId());
                    value.put("semanticRole", candidate.elementKey().semanticRole());
                    value.put("action", candidate.action().name());
                    value.put("status", item.status().name());
                    return value;
                })
                .sorted(Comparator.comparing(item -> item.get("pageId") + item.get("semanticRole")))
                .toList();
        JsonNode actual = mapper.valueToTree(Map.of(
                "schemaVersion", "golden-interaction-shape.v1",
                "interactions", interactions));
        JsonNode expected = mapper.readTree(Files.readString(Path.of(
                "demo", "orangehrm-login-logout", "expected", "canonical-interaction-shape.json")));

        Assert.assertTrue(actual.equals(expected), "Golden interaction snapshot mismatch:\nactual=" + actual
                + "\nexpected=" + expected);
        Assert.assertTrue(bundle.invariants().passed());
        Assert.assertTrue(bundle.requirementSelection().unresolvedRequirements().isEmpty());
    }

    private UiInteractionInventory inventory() {
        SemanticComponentInventory form = component("login:form", "LoginForm", ComponentType.FORM,
                List.of(
                        locator("username-locator", "login:form", "username", "name", "username"),
                        locator("password-locator", "login:form", "password", "name", "password"),
                        locator("submit-locator", "login:form", "login-button", "css", "button[type='submit']")),
                List.of(
                        action("username-action", "login:form", "TYPE", "username", "username-locator"),
                        action("password-action", "login:form", "TYPE", "password", "password-locator"),
                        action("submit-action", "login:form", "SUBMIT_FORM", "login-button", "submit-locator")));
        SemanticComponentInventory header = component("dashboard:header", "Header", ComponentType.HEADER,
                List.of(locator("menu-locator", "dashboard:header", "user-menu-trigger-2", "css", "span.user-menu")),
                List.of(action("menu-action-2", "dashboard:header", "OPEN_MENU", "user-menu-trigger-2", "menu-locator")));
        SemanticComponentInventory menu = component("dashboard:menu", "UserMenu", ComponentType.USER_MENU,
                List.of(locator("logout-locator", "dashboard:menu", "logout", "css", "a[href='/logout']")),
                List.of(action("logout-action", "dashboard:menu", "LOGOUT", "logout", "logout-locator")));
        return new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                List.of(
                        new UiInteractionPage("login", "LoginPage", "/login", "AUTHENTICATION", "fp-login", null,
                                List.of(form), List.of("golden")),
                        new UiInteractionPage("dashboard", "DashboardPage", "/dashboard", "AUTHENTICATED_AREA", "fp-dashboard", null,
                                List.of(header, menu), List.of("golden"))), List.of("golden"));
    }

    private SpaLiveTargetedVerificationResult live() {
        List<TargetedLocatorVerification> locators = List.of(
                verifiedLocator("login", "/login", "login:form", "username-locator", "username", "name", "username", "REQ-1"),
                verifiedLocator("login", "/login", "login:form", "password-locator", "password", "name", "password", "REQ-1"),
                verifiedLocator("login", "/login", "login:form", "submit-locator", "login-button", "css", "button[type='submit']", "REQ-1"),
                verifiedLocator("dashboard", "/dashboard", "dashboard:header", "menu-locator", "user-menu-trigger-2", "css", "span.user-menu", "REQ-3"),
                verifiedLocator("dashboard", "/dashboard", "dashboard:menu", "logout-locator", "logout", "css", "a[href='/logout']", "REQ-4"));
        List<TargetedActionVerification> actions = List.of(
                verifiedAction("login", "/login", "login:form", "username-action", "TYPE", "username", "REQ-1"),
                verifiedAction("login", "/login", "login:form", "password-action", "TYPE", "password", "REQ-1"),
                verifiedAction("login", "/login", "login:form", "submit-action", "SUBMIT_FORM", "login-button", "REQ-1"),
                verifiedAction("dashboard", "/dashboard", "dashboard:header", "menu-action-2", "OPEN_MENU", "user-menu-trigger-2", "REQ-3"),
                verifiedAction("dashboard", "/dashboard", "dashboard:menu", "logout-action", "LOGOUT", "logout", "REQ-4"));
        UiStateSnapshot dashboard = state("dashboard-base", "/dashboard");
        UiStateSnapshot menuOpen = state("dashboard-menu-open", "/dashboard");
        UiStateSnapshot loggedOut = state("login-after-logout", "/login");
        SpaStateGraph stateGraph = new SpaStateGraph(SpaStateGraph.SCHEMA_VERSION, null,
                List.of(dashboard, menuOpen, loggedOut),
                List.of(
                        transition("menu-transition", "dashboard-base", "dashboard-menu-open",
                                "menu-action-2", "OPEN_MENU", false, true),
                        transition("logout-transition", "dashboard-menu-open", "login-after-logout",
                                "logout-action", "LOGOUT", true, false)),
                List.of("golden"));
        return new SpaLiveTargetedVerificationResult(
                SpaLiveTargetedVerificationResult.SCHEMA_VERSION, null, true, true, locators, actions,
                stateGraph, List.of("golden"));
    }

    private List<StructuredBehaviorContract> requirements() {
        return List.of(
                requirement("REQ-1", "AUTHENTICATION", List.of("Authenticate"), "/login"),
                requirement("REQ-3", "USER_MENU", List.of("Open user menu"), "/dashboard"),
                requirement("REQ-4", "LOGOUT", List.of("Logout"), "/dashboard"));
    }

    private StructuredBehaviorContract requirement(String id, String capability, List<String> actions, String route) {
        return new StructuredBehaviorContract(id, capability, actions, List.of(), Map.of(),
                "route: " + route, true, List.of());
    }

    private SemanticComponentInventory component(String id, String name, ComponentType type,
                                                   List<CandidateLocatorEvidence> locators,
                                                   List<CandidateActionEvidence> actions) {
        return new SemanticComponentInventory(id, name, type, "", "", "", List.of(), 0.9d,
                locators, actions, List.of(), List.of("golden"));
    }

    private CandidateLocatorEvidence locator(String id, String component, String element, String strategy, String value) {
        return new CandidateLocatorEvidence(id, component, element, strategy, value, 0.9d, true,
                1, 1, true, true, true, LocatorEvidenceType.CANDIDATE_LOCATOR,
                SpaEvidenceStatus.CANDIDATE, List.of());
    }

    private CandidateActionEvidence action(String id, String component, String intent, String element, String locator) {
        return new CandidateActionEvidence(id, component, intent, element, 0.95d, List.of(locator),
                List.of(), List.of(), List.of("golden"), SpaEvidenceStatus.CANDIDATE);
    }

    private TargetedLocatorVerification verifiedLocator(String page, String route, String component,
                                                         String locator, String element, String strategy,
                                                         String value, String requirement) {
        return new TargetedLocatorVerification(page, route, "fp", component, locator, element, strategy,
                value, 0.95d, true, "passed", List.of(requirement));
    }

    private TargetedActionVerification verifiedAction(String page, String route, String component,
                                                       String action, String intent, String element,
                                                       String requirement) {
        return new TargetedActionVerification(page, route, "fp", component, action, intent, element,
                0.95d, true, "passed", List.of(requirement));
    }

    private UiStateSnapshot state(String id, String route) {
        return new UiStateSnapshot(id, "page", route, id + "-fp", null, List.of(), List.of(),
                List.of(), List.of(), false, true, !route.equals("/login"), 0.95d, List.of("golden"));
    }

    private UiStateTransition transition(String id, String from, String to, String actionId,
                                         String intent, boolean routeChanged, boolean sameRouteChange) {
        return new UiStateTransition(id, from, to, actionId, intent, "state changed", routeChanged,
                sameRouteChange, 0.95d, null, List.of("golden"));
    }
}
