package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.spa.SourceStateBindingService;
import ua.demo.agentlab.ui.discovery.spa.SpaTargetedVerificationPlanner;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTransitionDiscovery;
import ua.demo.agentlab.ui.discovery.spa.model.RequirementStateTransition;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SourceStateBindingServiceTest {

    @Test
    public void bindsDirectLogoutToNavigationWithoutUserMenuAssumption() {
        CandidateLocatorEvidence logout = locator("secure:navigation:logout", "secure:navigation",
                "logoutControl", "a.button[href='/logout']");
        CandidateActionEvidence logoutAction = action("secure:navigation:logout-action", "secure:navigation",
                "LOGOUT", "logoutControl", logout.locatorId());
        SemanticComponentInventory navigation = component("secure:navigation", ComponentType.NAVIGATION,
                List.of(logout), List.of(logoutAction));
        StructuredBehaviorContract requirement = new StructuredBehaviorContract("REQ-004", "LOGOUT",
                List.of("Logout from the authenticated area"), List.of(), Map.of(),
                "sourceRoute: /secure; targetRoute: /login; componentCapability: NAVIGATION; "
                        + "logoutAccessMode: DIRECT_CONTROL", true, List.of());

        SourceStateBinding binding = new SourceStateBindingService().bind(theInternetProfile(), List.of(requirement),
                inventory("secure", "SecureAreaPage", "/secure", List.of(navigation)), targetedConfig())
                .bindings().get(0);

        Assert.assertTrue(binding.liveVerificationEligible(), binding.reviewReasons().toString());
        Assert.assertEquals(binding.componentIds(), List.of("secure:navigation"));
        Assert.assertEquals(binding.candidateLocatorIds(), List.of(logout.locatorId()));
        Assert.assertEquals(binding.candidateActionIds(), List.of(logoutAction.actionId()));
    }

    @Test
    public void bindsUserMenuLogoutAsOrderedHeaderAndMenuEvidence() {
        CandidateLocatorEvidence opener = locator("dashboard:header:user-menu", "dashboard:header",
                "userMenuTrigger", "button[aria-haspopup='menu']");
        CandidateLocatorEvidence logout = locator("dashboard:user-menu:logout", "dashboard:user-menu",
                "logoutControl", "a[href='/auth/logout']");
        CandidateActionEvidence openMenu = action("dashboard:header:open-menu", "dashboard:header",
                "OPEN_MENU", "userMenuTrigger", opener.locatorId());
        CandidateActionEvidence logoutAction = action("dashboard:user-menu:logout-action", "dashboard:user-menu",
                "LOGOUT", "logoutControl", logout.locatorId());
        SemanticComponentInventory header = component("dashboard:header", ComponentType.HEADER,
                List.of(opener), List.of(openMenu));
        SemanticComponentInventory userMenu = component("dashboard:user-menu", ComponentType.USER_MENU,
                List.of(logout), List.of(logoutAction));
        StructuredBehaviorContract requirement = new StructuredBehaviorContract("REQ-004", "LOGOUT",
                List.of("Open the user menu", "Logout from the authenticated area"), List.of(), Map.of(),
                "sourceRoute: /dashboard/index; targetRoute: /auth/login; "
                        + "componentCapability: HEADER, USER_MENU; logoutAccessMode: USER_MENU", true, List.of());

        SourceStateBinding binding = new SourceStateBindingService().bind(profile(), List.of(requirement),
                inventory("dashboard", "DashboardPage", "/dashboard/index", List.of(header, userMenu)),
                targetedConfig()).bindings().get(0);

        Assert.assertTrue(binding.liveVerificationEligible(), binding.reviewReasons().toString());
        Assert.assertEquals(Set.copyOf(binding.componentIds()), Set.of("dashboard:header", "dashboard:user-menu"));
        Assert.assertEquals(Set.copyOf(binding.candidateLocatorIds()), Set.of(opener.locatorId(), logout.locatorId()));
        Assert.assertEquals(Set.copyOf(binding.candidateActionIds()), Set.of(openMenu.actionId(), logoutAction.actionId()));
    }

    @Test
    public void admitsRequirementRelevantCandidateBelowPromotionThresholdForLiveVerification() {
        CandidateLocatorEvidence recruitment = new CandidateLocatorEvidence(
                "dashboard:navigation:recruitment", "dashboard:navigation", "recruitmentLink", "css",
                "a[href='/recruitment/viewCandidates']", 0.72d, true, 1, 1, true, true, false,
                LocatorEvidenceType.CANDIDATE_LOCATOR, SpaEvidenceStatus.CANDIDATE, List.of("unstable-discovery"));
        CandidateLocatorEvidence admin = new CandidateLocatorEvidence(
                "dashboard:navigation:admin", "dashboard:navigation", "adminLink", "css",
                "a[href='/admin']", 0.90d, true, 1, 1, true, true, true,
                LocatorEvidenceType.CONFIRMED_LOCATOR, SpaEvidenceStatus.CONFIRMED, List.of());
        CandidateLocatorEvidence recruitmentText = new CandidateLocatorEvidence(
                "dashboard:navigation:recruitment-text", "dashboard:navigation", "recruitmentLink", "xpath",
                "//a[normalize-space()='Recruitment']", 0.85d, false, 1, 1, true, true, true,
                LocatorEvidenceType.CANDIDATE_LOCATOR, SpaEvidenceStatus.CANDIDATE, List.of("text-only-duplicate-risk"));
        CandidateLocatorEvidence recruitmentDuplicate = new CandidateLocatorEvidence(
                "dashboard:navigation:recruitment-duplicate", "dashboard:navigation", "recruitmentLinkDuplicate", "css",
                "a[href='/recruitment/viewCandidates']", 0.71d, true, 1, 1, true, true, false,
                LocatorEvidenceType.CANDIDATE_LOCATOR, SpaEvidenceStatus.CANDIDATE, List.of("unstable-discovery"));
        CandidateActionEvidence openRecruitment = new CandidateActionEvidence(
                "dashboard:navigation:open-recruitment", "dashboard:navigation", "CLICK", "recruitmentLink", 0.80d,
                List.of(recruitment.locatorId(), recruitmentText.locatorId()), List.of(), List.of(), List.of(), SpaEvidenceStatus.CANDIDATE);
        CandidateActionEvidence duplicateAction = new CandidateActionEvidence(
                "dashboard:navigation:open-recruitment-duplicate", "dashboard:navigation", "CLICK",
                "recruitmentLinkDuplicate", 0.79d, List.of(recruitmentDuplicate.locatorId()), List.of(), List.of(),
                List.of(), SpaEvidenceStatus.CANDIDATE);
        SemanticComponentInventory navigation = new SemanticComponentInventory(
                "dashboard:navigation", "Navigation", ComponentType.NAVIGATION, "css", "nav", "", List.of(), 0.90d,
                List.of(recruitment, recruitmentText, recruitmentDuplicate, admin),
                List.of(openRecruitment, duplicateAction), List.of(), List.of());
        UiInteractionInventory inventory = new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                List.of(new UiInteractionPage("dashboard", "DashboardPage", "/dashboard", "AUTHENTICATED_AREA", "fp",
                        null, List.of(navigation), List.of())), List.of());
        StructuredBehaviorContract requirement = new StructuredBehaviorContract("REQ-1", "MODULE_NAVIGATION",
                List.of("Open the Recruitment module from application navigation."), List.of(), Map.of(),
                "pageCapability: AUTHENTICATED_AREA; componentCapability: NAVIGATION; "
                        + "sourceRoute: project-profile.authenticatedRoute; targetPage: discovery-confirmed Recruitment page",
                true, List.of());

        var result = new SourceStateBindingService().bind(profile(), List.of(requirement), inventory,
                new SpaInventoryConfig(true, SpaDiscoveryMode.TARGETED, 30, true, 0.80d, 2, 2));

        Assert.assertEquals(result.bindings().size(), 1);
        Assert.assertTrue(result.bindings().get(0).liveVerificationEligible(), result.bindings().get(0).reviewReasons().toString());
        Assert.assertEquals(result.bindings().get(0).candidateLocatorIds(), List.of(recruitment.locatorId()));
        Assert.assertEquals(result.bindings().get(0).candidateActionIds(), List.of(openRecruitment.actionId()));
        var planned = new SpaTargetedVerificationPlanner().verify(inventory,
                new CanonicalTestCaseBundle("test", "", List.of(), List.of()), List.of(requirement), result,
                new SpaInventoryConfig(true, SpaDiscoveryMode.TARGETED, 30, true, 0.80d, 2, 2));
        Assert.assertTrue(planned.locatorVerifications().get(0).verified());
        Assert.assertEquals(planned.locatorVerifications().get(0).qualityScore(), 0.72d);
        Assert.assertTrue(planned.actionVerifications().get(0).verified());
    }

    @Test
    public void resolvesFlattenedDiscoveryConfirmedSourceContextToMatchingInventoryPage() {
        UiInteractionPage dashboard = new UiInteractionPage("dashboard", "DashboardPage", "/dashboard", "AUTHENTICATED_AREA",
                "fp", null, List.of(), List.of());
        UiInteractionPage recruitment = new UiInteractionPage("recruitment", "RecruitmentPage", "/recruitment/viewCandidates",
                "AUTHENTICATED_AREA", "fp2", null, List.of(), List.of());
        StructuredBehaviorContract requirement = new StructuredBehaviorContract("REQ-2", "MODULE_NAVIGATION",
                List.of("Open the Vacancies navigation option."), List.of(), Map.of(),
                "pageCapability: RECORD_LIST componentCapability: NAVIGATION sourceRoute: discovery-confirmed recruitment route "
                        + "targetRoute: discovery-confirmed vacancies route targetPage: discovery-confirmed Vacancies page",
                true, List.of());

        var result = new SourceStateBindingService().bind(profile(), List.of(requirement),
                new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                        List.of(dashboard, recruitment), List.of()),
                new SpaInventoryConfig(true, SpaDiscoveryMode.TARGETED, 30, true, 0.80d, 2, 2));

        Assert.assertEquals(result.bindings().get(0).sourcePageId(), "recruitment");
        Assert.assertEquals(result.bindings().get(0).sourceRoute(), "/recruitment/viewCandidates");
    }

    @Test
    public void admitsPluralTargetLocatorAfterCanonicalTokenNormalization() {
        CandidateLocatorEvidence vacancies = new CandidateLocatorEvidence(
                "recruitment:navigation:vacancies", "recruitment:navigation", "vacanciesLink", "xpath",
                "//a[normalize-space()='Vacancies']", 0.706d, true, 1, 1, true, true, false,
                LocatorEvidenceType.CANDIDATE_LOCATOR, SpaEvidenceStatus.CANDIDATE,
                List.of("UNSTABLE_DISCOVERY", "text-only-duplicate-risk"));
        CandidateActionEvidence openVacancies = new CandidateActionEvidence(
                "recruitment:navigation:open-vacancies", "recruitment:navigation", "CLICK", "vacanciesLink",
                0.80d, List.of(vacancies.locatorId()), List.of(), List.of(), List.of(), SpaEvidenceStatus.CANDIDATE);
        SemanticComponentInventory navigation = new SemanticComponentInventory(
                "recruitment:navigation", "Navigation", ComponentType.NAVIGATION, "css", "nav", "", List.of(),
                0.90d, List.of(vacancies), List.of(openVacancies), List.of(), List.of());
        UiInteractionPage recruitment = new UiInteractionPage("web-index-php-recruitment-viewcandidates",
                "RecruitmentPage", "/recruitment/viewCandidates", "MODULE_NAVIGATION", "fp", null,
                List.of(navigation), List.of());
        StructuredBehaviorContract requirement = new StructuredBehaviorContract("REQ-2", "MODULE_NAVIGATION",
                List.of("Open the Vacancies navigation option."), List.of(), Map.of(),
                "pageCapability: RECORD_LIST; componentCapability: NAVIGATION; "
                        + "sourceRoute: discovery-confirmed recruitment route; "
                        + "targetRoute: discovery-confirmed vacancies route; "
                        + "targetPage: discovery-confirmed Vacancies page",
                true, List.of());

        var result = new SourceStateBindingService().bind(profile(), List.of(requirement),
                new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                        List.of(recruitment), List.of()),
                new SpaInventoryConfig(true, SpaDiscoveryMode.TARGETED, 30, true, 0.80d, 0.65d,
                        2, 2, true, false, true, false, true, 14, 30, false));

        Assert.assertTrue(result.bindings().get(0).liveVerificationEligible(),
                result.bindings().get(0).reviewReasons().toString());
        Assert.assertEquals(result.bindings().get(0).candidateLocatorIds(), List.of(vacancies.locatorId()));
        Assert.assertEquals(result.bindings().get(0).candidateActionIds(), List.of(openVacancies.actionId()));
    }

    @Test
    public void confirmedTransitionOverridesAmbiguousMultiHopSourceBinding() {
        UiInteractionPage dashboard = new UiInteractionPage("dashboard", "DashboardPage", "/dashboard",
                "AUTHENTICATED_AREA", "dashboard-fp", null, List.of(), List.of());
        CandidateLocatorEvidence vacancies = new CandidateLocatorEvidence(
                "recruitment:navigation:vacancies", "recruitment:navigation", "vacanciesLink", "css",
                "a[href='/recruitment/viewJobVacancy']", 0.82d, true, 1, 1, true, true, true,
                LocatorEvidenceType.CONFIRMED_LOCATOR, SpaEvidenceStatus.CONFIRMED, List.of());
        CandidateActionEvidence openVacancies = new CandidateActionEvidence(
                "recruitment:navigation:open-vacancies", "recruitment:navigation", "CLICK", "vacanciesLink",
                0.88d, List.of(vacancies.locatorId()), List.of(), List.of(), List.of(), SpaEvidenceStatus.CONFIRMED);
        SemanticComponentInventory navigation = new SemanticComponentInventory(
                "recruitment:navigation", "Navigation", ComponentType.NAVIGATION, "css", "nav", "",
                List.of(), 0.90d, List.of(vacancies), List.of(openVacancies), List.of(), List.of());
        UiInteractionPage recruitment = new UiInteractionPage("recruitment", "RecruitmentPage",
                "/recruitment/viewCandidates", "MODULE_NAVIGATION", "recruitment-fp", null,
                List.of(navigation), List.of());
        SourceStateBinding stale = new SourceStateBinding("REQ-2", "MODULE_NAVIGATION", "dashboard", "/dashboard",
                "Vacancies", List.of(), List.of(), List.of(), false, List.of("ambiguous semantic source"));
        SourceStateBindingBundle current = new SourceStateBindingBundle(SourceStateBindingBundle.SCHEMA_VERSION,
                null, List.of(stale), List.of());
        LiveTransitionDiscovery discovery = new LiveTransitionDiscovery(LiveTransitionDiscovery.SCHEMA_VERSION, null,
                new SpaLiveTargetedVerificationResult(SpaLiveTargetedVerificationResult.SCHEMA_VERSION, null,
                        true, true, List.of(), List.of(), List.of()),
                List.of(new RequirementStateTransition("REQ-2", "recruitment", "/recruitment/viewCandidates",
                        openVacancies.actionId(), vacancies.locatorId(), "vacancies-state",
                        "/recruitment/viewJobVacancy", true, "live transition confirmed")), List.of());

        SourceStateBinding rebound = new SourceStateBindingService().rebindConfirmedTransitions(current, discovery,
                new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                        List.of(dashboard, recruitment), List.of())).bindings().get(0);

        Assert.assertEquals(rebound.sourcePageId(), "recruitment");
        Assert.assertEquals(rebound.sourceRoute(), "/recruitment/viewCandidates");
        Assert.assertEquals(rebound.componentIds(), List.of("recruitment:navigation"));
        Assert.assertEquals(rebound.candidateLocatorIds(), List.of(vacancies.locatorId()));
        Assert.assertEquals(rebound.candidateActionIds(), List.of(openVacancies.actionId()));
        Assert.assertTrue(rebound.liveVerificationEligible());
        Assert.assertTrue(rebound.reviewReasons().isEmpty());
    }

    private ProjectProfile profile() {
        return new ProjectProfile("test", "Test", "https://example.test", "/login", "/login", "", "/dashboard",
                "", "", "", "", "", "", "", new OutputProfile("pages", "tests"));
    }

    private ProjectProfile theInternetProfile() {
        return new ProjectProfile("the-internet", "The Internet", "https://the-internet.herokuapp.com",
                "/login", "/login", "", "/secure", "", "", "", "", "", "", "",
                new OutputProfile("pages", "tests"));
    }

    private UiInteractionInventory inventory(String pageId, String pageName, String route,
                                             List<SemanticComponentInventory> components) {
        return new UiInteractionInventory(UiInteractionInventory.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                List.of(new UiInteractionPage(pageId, pageName, route, "AUTHENTICATED_AREA|LOGOUT", "fp",
                        null, components, List.of())), List.of());
    }

    private SpaInventoryConfig targetedConfig() {
        return new SpaInventoryConfig(false, SpaDiscoveryMode.TARGETED, 30, true, 0.80d, 2, 2);
    }

    private CandidateLocatorEvidence locator(String locatorId, String componentId, String elementId, String value) {
        return new CandidateLocatorEvidence(locatorId, componentId, elementId, "css", value, 0.90d,
                true, 1, 1, true, true, true, LocatorEvidenceType.CONFIRMED_LOCATOR,
                SpaEvidenceStatus.CONFIRMED, List.of());
    }

    private CandidateActionEvidence action(String actionId, String componentId, String intent,
                                           String elementId, String locatorId) {
        return new CandidateActionEvidence(actionId, componentId, intent, elementId, 0.92d,
                List.of(locatorId), List.of(), List.of(), List.of(), SpaEvidenceStatus.CONFIRMED);
    }

    private SemanticComponentInventory component(String componentId, ComponentType type,
                                                  List<CandidateLocatorEvidence> locators,
                                                  List<CandidateActionEvidence> actions) {
        return new SemanticComponentInventory(componentId, componentId, type, "css", "body", "", List.of(),
                0.90d, locators, actions, List.of(), List.of());
    }
}
