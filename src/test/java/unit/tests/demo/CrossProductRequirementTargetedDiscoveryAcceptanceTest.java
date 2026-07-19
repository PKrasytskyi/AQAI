package unit.tests.demo;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContractBuilder;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.requirements.normalization.RuleBasedRequirementNormalizer;
import ua.demo.agentlab.requirements.source.FileRequirementSource;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelAssembler;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelInput;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelReport;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceRequirementResult;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorStep;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTransitionDiscovery;
import ua.demo.agentlab.ui.discovery.spa.model.RequirementStateTransition;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage;
import ua.demo.agentlab.ui.discovery.spa.model.SpaStateGraph;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** BW-04 acceptance over the production source/transition/target/funnel contracts. */
public class CrossProductRequirementTargetedDiscoveryAcceptanceTest {

    @DataProvider
    public Object[][] fixtures() {
        return new Object[][]{
                {"requirements/orangehrm-authentication-user-menu-logout.md", "login", "LoginPage", "/auth/login",
                        "dashboard", "DashboardPage", "/dashboard/index", "USER_MENU", "OPEN_USER_MENU"},
                {"requirements/the-internet-authentication-logout.md", "login", "LoginPage", "/login",
                        "secure", "SecureAreaPage", "/secure", "NAVIGATION", "INSPECT_PAGE_CONTENT"}
        };
    }

    @Test(dataProvider = "fixtures")
    public void everyPrimaryRequirementCarriesTargetedDiscoveryTraceOrOnePreciseStop(
            String requirementFile,
            String loginPageId,
            String loginPageName,
            String loginRoute,
            String authenticatedPageId,
            String authenticatedPageName,
            String authenticatedRoute,
            String logoutComponentCapability,
            String accessIntent
    ) {
        List<StructuredBehaviorContract> requirements = requirements(requirementFile);
        UiInteractionInventory inventory = inventory(loginPageId, loginPageName, loginRoute,
                authenticatedPageId, authenticatedPageName, authenticatedRoute, logoutComponentCapability);
        List<SourceStateBinding> sources = new ArrayList<>();
        List<RequirementStateTransition> transitions = new ArrayList<>();
        List<TargetStateBinding> targets = new ArrayList<>();
        List<BoundSpaBehaviorContract> behaviors = new ArrayList<>();

        for (StructuredBehaviorContract requirement : requirements) {
            FixtureTrace trace = traceFor(requirement.requirementId(), loginPageId, loginRoute,
                    authenticatedPageId, authenticatedRoute, logoutComponentCapability);
            String actionId = "action-" + requirement.requirementId().toLowerCase(java.util.Locale.ROOT);
            sources.add(new SourceStateBinding(requirement.requirementId(), requirement.capability(),
                    trace.sourcePageId(), trace.sourceRoute(), trace.targetRoute(), trace.componentIds(),
                    List.of(), List.of(actionId), true, List.of()));
            transitions.add(new RequirementStateTransition(requirement.requirementId(), trace.sourcePageId(),
                    trace.sourceRoute(), actionId, "", trace.targetStateId(), trace.targetRoute(), true, ""));
            targets.add(new TargetStateBinding(requirement.requirementId(), requirement.capability(),
                    trace.sourcePageId(), trace.sourceRoute(), trace.targetStateId(), trace.targetRoute(), actionId,
                    "", true, List.of()));
            behaviors.add(new BoundSpaBehaviorContract(requirement.requirementId(), requirement.capability(),
                    trace.sourcePageId(), trace.sourceRoute(), "flow-" + requirement.requirementId(),
                    trace.componentIds(), List.of(new BoundSpaBehaviorStep(trace.stepKind(), actionId, "", "", "")),
                    List.of(), Map.of(), true, List.of()));
        }

        SpaLiveTargetedVerificationResult verification = verification(loginPageId, loginRoute,
                authenticatedPageId, authenticatedRoute);
        UiEvidenceFunnelReport report = new UiEvidenceFunnelAssembler().assemble(new UiEvidenceFunnelInput(
                "bw04-" + authenticatedPageId,
                requirements,
                inventory,
                new SourceStateBindingBundle(SourceStateBindingBundle.SCHEMA_VERSION, null, sources, List.of()),
                new LiveTransitionDiscovery(LiveTransitionDiscovery.SCHEMA_VERSION, null, verification,
                        transitions, List.of("bw04:live-transition-discovery")),
                behaviors,
                verification,
                new TargetStateBindingBundle(TargetStateBindingBundle.SCHEMA_VERSION, null, targets,
                        behaviors, List.of()),
                emptyContext()
        ));

        Assert.assertTrue(report.completenessPassed());
        Assert.assertEquals(report.requirements().size(), 4);
        for (UiEvidenceRequirementResult result : report.requirements()) {
            Assert.assertFalse(result.trace().source().pageId().isBlank(), result.requirementId());
            Assert.assertFalse(result.trace().source().route().isBlank(), result.requirementId());
            Assert.assertFalse(result.trace().source().stateId().isBlank(), result.requirementId());
            Assert.assertFalse(result.trace().requiredComponentCapabilities().isEmpty(), result.requirementId());
            Assert.assertFalse(result.trace().expectedActionIntents().isEmpty(), result.requirementId());
            Assert.assertFalse(result.trace().target().route().isBlank(), result.requirementId());
            Assert.assertFalse(result.trace().target().stateId().isBlank(), result.requirementId());
            Assert.assertEquals(result.trace().discoveryRouteSource(), "LIVE_TRANSITION_DISCOVERY");
            Assert.assertTrue(result.confirmedEvidencePath() || result.hasExplicitStop(), result.requirementId());
        }
        UiEvidenceRequirementResult access = result(report, "REQ-003");
        Assert.assertTrue(access.trace().requiredComponentCapabilities().contains(logoutComponentCapability));
        Assert.assertTrue(access.trace().expectedActionIntents().contains(accessIntent), access.trace().toString());
        Assert.assertFalse(report.requirements().stream()
                .filter(result -> !result.confirmedEvidencePath())
                .anyMatch(result -> result.stoppedAt().isBlank() || result.reason().isBlank()
                        || result.remediation().isBlank()));
    }

    private List<StructuredBehaviorContract> requirements(String requirementFile) {
        var normalized = new RuleBasedRequirementNormalizer().normalize(
                new FileRequirementSource().load(new RequirementInput(SourceType.FILE, requirementFile))
        );
        return new StructuredBehaviorContractBuilder().build(normalized.requirements());
    }

    private UiInteractionInventory inventory(
            String loginPageId,
            String loginPageName,
            String loginRoute,
            String authenticatedPageId,
            String authenticatedPageName,
            String authenticatedRoute,
            String logoutComponentCapability
    ) {
        SemanticComponentInventory form = component("login-form", ComponentType.FORM);
        ComponentType logoutType = logoutComponentCapability.equals("USER_MENU")
                ? ComponentType.USER_MENU : ComponentType.NAVIGATION;
        UiInteractionPage login = new UiInteractionPage(loginPageId, loginPageName, loginRoute,
                "AUTHENTICATION", "state-login", null, List.of(form), List.of("bw04:fixture"));
        UiInteractionPage authenticated = new UiInteractionPage(authenticatedPageId, authenticatedPageName,
                authenticatedRoute, "AUTHENTICATED_AREA|LOGOUT", "state-authenticated", null,
                List.of(component("authenticated-content", ComponentType.CONTENT),
                        component("logout-access", logoutType)), List.of("bw04:fixture"));
        return new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                List.of(login, authenticated), List.of("bw04:production-contract-fixture"));
    }

    private SemanticComponentInventory component(String id, ComponentType type) {
        return new SemanticComponentInventory(id, id, type, "", "", "", List.of(), 0.95d,
                List.of(), List.of(), List.of(), List.of("bw04:component"));
    }

    private SpaLiveTargetedVerificationResult verification(
            String loginPageId,
            String loginRoute,
            String authenticatedPageId,
            String authenticatedRoute
    ) {
        UiStateSnapshot login = new UiStateSnapshot("state-login", loginPageId, loginRoute, "state-login",
                null, List.of("login-form"), List.of(), List.of(), List.of(), false, true, false, 0.98d, List.of());
        UiStateSnapshot authenticated = new UiStateSnapshot("state-authenticated", authenticatedPageId,
                authenticatedRoute, "state-authenticated", null,
                List.of("authenticated-content", "logout-access"), List.of(), List.of(), List.of(),
                false, true, true, 0.98d, List.of());
        return new SpaLiveTargetedVerificationResult(SpaLiveTargetedVerificationResult.SCHEMA_VERSION, null,
                true, true, List.of(), List.of(), new SpaStateGraph(SpaStateGraph.SCHEMA_VERSION, null,
                List.of(login, authenticated), List.of(), List.of("bw04:states")), List.of());
    }

    private FixtureTrace traceFor(
            String requirementId,
            String loginPageId,
            String loginRoute,
            String authenticatedPageId,
            String authenticatedRoute,
            String logoutComponentCapability
    ) {
        return switch (requirementId) {
            case "REQ-001" -> new FixtureTrace(loginPageId, loginRoute, loginRoute, "state-login",
                    List.of("login-form"), "OPEN_ROUTE");
            case "REQ-002" -> new FixtureTrace(loginPageId, loginRoute, authenticatedRoute, "state-authenticated",
                    List.of("login-form", "authenticated-content"), "AUTHENTICATE");
            case "REQ-003" -> new FixtureTrace(authenticatedPageId, authenticatedRoute, authenticatedRoute,
                    "state-authenticated", List.of("logout-access"),
                    logoutComponentCapability.equals("USER_MENU") ? "OPEN_USER_MENU" : "INSPECT_PAGE_CONTENT");
            case "REQ-004" -> new FixtureTrace(authenticatedPageId, authenticatedRoute, loginRoute, "state-login",
                    List.of("logout-access", "login-form"), "LOGOUT");
            default -> throw new IllegalArgumentException("Unexpected demo requirement " + requirementId);
        };
    }

    private UiEvidenceRequirementResult result(UiEvidenceFunnelReport report, String requirementId) {
        return report.requirements().stream().filter(result -> result.requirementId().equals(requirementId))
                .findFirst().orElseThrow();
    }

    private AiContextPackage emptyContext() {
        return new AiContextPackage("", null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), PromptUiEvidence.empty("bw04:no-prompt-evidence"));
    }

    private record FixtureTrace(
            String sourcePageId,
            String sourceRoute,
            String targetRoute,
            String targetStateId,
            List<String> componentIds,
            String stepKind
    ) {
    }
}
