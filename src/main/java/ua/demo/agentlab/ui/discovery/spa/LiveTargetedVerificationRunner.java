package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.core.config.PropertiesUiRuntimeConfig;
import ua.demo.agentlab.core.ui.driver.DefaultDriverFactory;
import ua.demo.agentlab.core.ui.driver.DriverFactory;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationConfig;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationService;
import ua.demo.agentlab.ui.discovery.selenium.auth.RouteProtectionResolver;
import ua.demo.agentlab.ui.discovery.evidence.LocalPageEvidenceCaptureService;
import ua.demo.agentlab.ui.discovery.browser.BrowserCapabilityAdapterRegistry;
import ua.demo.agentlab.ui.discovery.evidence.PageEvidenceCaptureService;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.selenium.collector.PageSnapshotCollector;
import ua.demo.agentlab.ui.discovery.selenium.extractor.FormStructureExtractor;
import ua.demo.agentlab.ui.discovery.selenium.extractor.InteractiveElementExtractor;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessRuleResolver;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessWaiter;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.ComponentActionDependency;
import ua.demo.agentlab.ui.discovery.spa.model.ComponentInteractionGraph;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage;
import ua.demo.agentlab.ui.discovery.spa.model.SpaStateGraph;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTargetPageSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateTransition;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Replays only requirement-selected candidate evidence in a fresh browser session.
 * It never explores an entire SPA and never executes destructive actions. Session-ending actions
 * are opt-in because a target may share credentials with other verification work.
 */
public class LiveTargetedVerificationRunner {

    private final ComponentActionDependencyResolver dependencyResolver = new ComponentActionDependencyResolver();
    private final DriverFactory driverFactory;
    private final DiscoveryAuthenticationService authentication;
    private final RouteProtectionResolver routeProtectionResolver = new RouteProtectionResolver();
    private final AuthenticationPreconditionExecutor preconditions;
    private final PageReadinessRuleResolver readinessResolver;
    private final PageReadinessWaiter readinessWaiter;
    private final SpaStateSnapshotCaptureService stateSnapshots;
    private final UiStateSnapshotCollector snapshotCollector;
    private final StateTransitionCaptureService transitionCapture = new StateTransitionCaptureService();
    private final LocatorRuntimeVerifier locatorVerifier = new LocatorRuntimeVerifier();
    private final SafeActionExecutor actionExecutor;
    private final LiveVerificationResultAssembler resultAssembler = new LiveVerificationResultAssembler();
    private final PageReadinessVerifier pageReadiness;
    private final PageSnapshotCollector targetPageCollector;
    private final PageEvidenceCaptureService targetPageEvidence;
    private final BrowserCapabilityAdapterRegistry browserCapabilities = new BrowserCapabilityAdapterRegistry();
    private final DiscoveryAuthenticationConfig authenticationConfig = new DiscoveryAuthenticationConfig();

    public LiveTargetedVerificationRunner() {
        this(new DefaultDriverFactory(new PropertiesUiRuntimeConfig()),
                new DiscoveryAuthenticationService(new DiscoveryAuthenticationConfig()),
                new PageReadinessRuleResolver(), new PageReadinessWaiter(), new SpaStateSnapshotCaptureService(),
                defaultPageCollector(), new LocalPageEvidenceCaptureService());
    }

    public LiveTargetedVerificationRunner(DriverFactory driverFactory,
                                          DiscoveryAuthenticationService authentication,
                                          PageReadinessRuleResolver readinessResolver,
                                          PageReadinessWaiter readinessWaiter) {
        this(driverFactory, authentication, readinessResolver, readinessWaiter, new SpaStateSnapshotCaptureService(),
                defaultPageCollector(), new LocalPageEvidenceCaptureService());
    }

    public LiveTargetedVerificationRunner(DriverFactory driverFactory,
                                          DiscoveryAuthenticationService authentication,
                                          PageReadinessRuleResolver readinessResolver,
                                          PageReadinessWaiter readinessWaiter,
                                          SpaStateSnapshotCaptureService stateSnapshots) {
        this(driverFactory, authentication, readinessResolver, readinessWaiter, stateSnapshots,
                defaultPageCollector(), new LocalPageEvidenceCaptureService());
    }

    LiveTargetedVerificationRunner(DriverFactory driverFactory,
                                   DiscoveryAuthenticationService authentication,
                                   PageReadinessRuleResolver readinessResolver,
                                   PageReadinessWaiter readinessWaiter,
                                   SpaStateSnapshotCaptureService stateSnapshots,
                                   PageSnapshotCollector targetPageCollector,
                                   PageEvidenceCaptureService targetPageEvidence) {
        this.driverFactory = driverFactory;
        this.authentication = authentication;
        this.readinessResolver = readinessResolver;
        this.readinessWaiter = readinessWaiter;
        this.stateSnapshots = stateSnapshots == null ? new SpaStateSnapshotCaptureService() : stateSnapshots;
        this.snapshotCollector = new UiStateSnapshotCollector(this.stateSnapshots);
        this.pageReadiness = new PageReadinessVerifier(readinessResolver, readinessWaiter);
        this.actionExecutor = new SafeActionExecutor(authentication, browserCapabilities, authenticationConfig,
                locatorVerifier);
        this.preconditions = new AuthenticationPreconditionExecutor(authentication, routeProtectionResolver);
        this.targetPageCollector = targetPageCollector == null ? defaultPageCollector() : targetPageCollector;
        this.targetPageEvidence = targetPageEvidence == null ? new LocalPageEvidenceCaptureService() : targetPageEvidence;
    }

    public SpaLiveTargetedVerificationResult verify(ProjectProfile profile,
                                                     UiInteractionInventory inventory,
                                                     SpaTargetedVerificationResult planned,
                                                     ComponentInteractionGraph graph,
                                                     SpaInventoryConfig config) {
        if (profile == null || inventory == null || planned == null || !config.liveVerificationEnabled()) {
            return SpaLiveTargetedVerificationResult.skipped(planned == null ? null : planned.runMetadata(),
                    "spa-live-verification:disabled-or-missing-input");
        }
        if (planned.locatorVerifications().isEmpty() && planned.actionVerifications().isEmpty()) {
            return SpaLiveTargetedVerificationResult.skipped(planned.runMetadata(), "spa-live-verification:no-planned-evidence");
        }
        List<TargetedLocatorVerification> locators = new ArrayList<>();
        List<TargetedActionVerification> actions = new ArrayList<>();
        List<String> trace = new ArrayList<>(List.of("spa-live-verification:fresh-browser", "profile=" + profile.profileId()));
        List<UiStateSnapshot> states = new ArrayList<>();
        List<UiStateTransition> transitions = new ArrayList<>();
        List<LiveTargetPageSnapshot> targetPages = new ArrayList<>();
        LiveAuthenticationState authenticationState = new LiveAuthenticationState();
        try (BrowserVerificationSession session = new BrowserVerificationSession(driverFactory)) {
            WebDriver driver = session.open();
            Map<String, UiInteractionPage> pages = inventory.pages().stream()
                    .collect(java.util.stream.Collectors.toMap(UiInteractionPage::pageId, page -> page, (left, right) -> left, LinkedHashMap::new));
            for (UiInteractionPage page : pages.values()) {
                List<TargetedLocatorVerification> pageLocators = planned.locatorVerifications().stream()
                        .filter(item -> page.pageId().equals(item.pageId())).toList();
                List<TargetedActionVerification> pageActions = planned.actionVerifications().stream()
                        .filter(item -> page.pageId().equals(item.pageId())).toList();
                if (pageLocators.isEmpty() && pageActions.isEmpty()) continue;

                String targetUrl = join(profile.baseUrl(), page.route());
                // Authenticate through the profile's confirmed authenticated route first. A module
                // route such as /recruitment/vacancies is not required to be hard-coded as a
                // protected route in the profile for the runner to validate it afterwards.
                var precondition = preconditions.satisfy(driver, profile, page, pageActions, authenticationState);
                boolean requiresAuthentication = precondition.required();
                if (!precondition.satisfied()) {
                    List<TargetedLocatorVerification> failedLocators = pageLocators.stream()
                            .map(item -> withLocatorResult(item, false, precondition.reason())).toList();
                    List<TargetedActionVerification> failedActions = pageActions.stream()
                            .map(item -> withActionResult(item, false, precondition.reason())).toList();
                    locators.addAll(failedLocators);
                    actions.addAll(failedActions);
                    continue;
                }
                if (precondition.authenticatedNow()) {
                    for (UiInteractionPage candidatePage : pages.values()) {
                        if (!RouteCanonicalizer.routeEqualsOrSuffix(candidatePage.route(), profile.authenticatedRoute())) continue;
                        UiStateSnapshot authenticatedState = snapshotCollector.capture(driver, profile, candidatePage);
                        snapshotCollector.addDistinct(states, authenticatedState);
                        trace.add("state-snapshot:authenticated-route=" + authenticatedState.route());
                        break;
                    }
                }
                try {
                    if (!currentRouteMatches(driver, page.route())) {
                        driver.navigate().to(targetUrl);
                        if (!requiresAuthentication && !authenticationState.authenticated()
                                && preconditions.redirectedToLogin(profile, page.route(), driver.getCurrentUrl())) {
                            if (!preconditions.authenticateAfterRedirect(driver, profile, authenticationState)) {
                                throw new IllegalStateException("live authentication failed after login redirect");
                            }
                            driver.navigate().to(targetUrl);
                            trace.add("route-protection:live-login-redirect=" + page.route());
                        }
                        trace.add("state-explorer:direct-route-fallback=" + page.route());
                    } else {
                        trace.add("state-explorer:reused-live-transition=" + page.route());
                    }
                    var readiness = pageReadiness.await(driver, profile, targetUrl, page.route());
                    if (!readiness.ready()) {
                        throw new IllegalStateException("Target page readiness failed: " + readiness.reason());
                    }
                    trace.add("route-validated=" + page.route());
                } catch (RuntimeException exception) {
                    String reason = "live route/readiness validation failed: " + concise(exception);
                    locators.addAll(pageLocators.stream().map(item -> withLocatorResult(item, false, reason)).toList());
                    actions.addAll(pageActions.stream().map(item -> withActionResult(item, false, reason)).toList());
                    trace.add("route-validation-failed=" + page.route() + " reason=" + concise(exception));
                    continue;
                }

                UiStateSnapshot currentState = snapshotCollector.capture(driver, profile, page);
                snapshotCollector.addDistinct(states, currentState);

                Map<String, TargetedLocatorVerification> liveLocatorById = new LinkedHashMap<>();
                for (TargetedLocatorVerification plannedLocator : pageLocators) {
                    TargetedLocatorVerification result = verifyLocator(driver, plannedLocator, config);
                    liveLocatorById.put(result.locatorId(), result);
                }
                Map<String, Boolean> actionOutcome = new LinkedHashMap<>();
                for (TargetedActionVerification plannedAction : ordered(pageActions, graph, page.pageId())) {
                    CandidateActionEvidence candidate = findAction(page, plannedAction.actionId());
                    TargetedActionVerification result = verifyAction(driver, profile, plannedAction, candidate,
                            liveLocatorById, actionOutcome, graph, config, page, pages);
                    authenticationState.observe(result.intent(), result.verified(),
                            currentRouteMatches(driver, profile.authenticatedRoute()),
                            config.executeSessionEndingActions());
                    actionOutcome.put(result.actionId(), result.verified());
                    actions.add(result);
                    if (result.verified() && isSafeStateAction(result.intent())) {
                        if ("OPEN_MENU".equalsIgnoreCase(result.intent())) {
                            refreshFailedLocators(driver, liveLocatorById, config);
                        }
                        LiveTargetPageSnapshot targetPage = captureTargetPage(driver, profile, page, result, planned, trace);
                        if (targetPage != null) addTargetPage(targetPages, targetPage);
                        String nextPageId = targetPage == null ? page.pageId() : targetPage.targetPageId();
                        UiStateSnapshot nextState = snapshotCollector.capture(driver, profile, nextPageId, planned.runMetadata());
                        snapshotCollector.addDistinct(states, nextState);
                        transitions.add(transitionCapture.capture(currentState, nextState, result));
                        currentState = nextState;
                    }
                }
                locators.addAll(liveLocatorById.values());
            }
            return resultAssembler.success(planned.runMetadata(), locators, actions, states, transitions,
                    targetPages, trace);
        } catch (RuntimeException exception) {
            trace.add("live-browser-error=" + concise(exception));
            return resultAssembler.failure(planned.runMetadata(), locators, actions, states, transitions,
                    targetPages, trace);
        }
    }

    private boolean currentRouteMatches(WebDriver driver, String route) {
        return pageReadiness.routeMatches(driver, route);
    }

    private LiveTargetPageSnapshot captureTargetPage(WebDriver driver, ProjectProfile profile, UiInteractionPage sourcePage,
                                                     TargetedActionVerification action,
                                                     SpaTargetedVerificationResult planned, List<String> trace) {
        String actualRoute = RouteCanonicalizer.canonicalize(driver.getCurrentUrl());
        if (actualRoute.isBlank() || RouteCanonicalizer.routeEqualsOrSuffix(actualRoute, sourcePage.route())) return null;
        String targetUrl = driver.getCurrentUrl();
        var readiness = pageReadiness.await(driver, profile, targetUrl, actualRoute);
        if (!readiness.ready()) {
            trace.add("target-state-capture-skipped=" + actualRoute + " reason=" + readiness.reason());
            return null;
        }
        String targetPageId = pageId(targetUrl);
        var evidence = targetPageEvidence.capture(driver, targetPageId);
        var snapshot = targetPageCollector.collect(driver, targetPageId, evidence);
        trace.add("target-state-captured=" + actualRoute + " elements=" + snapshot.rawElements().size());
        return new LiveTargetPageSnapshot(sourcePage.pageId(), sourcePage.route(), action.actionId(), targetPageId,
                actualRoute, action.requirementIds(), snapshot);
    }

    private void addTargetPage(List<LiveTargetPageSnapshot> pages, LiveTargetPageSnapshot page) {
        pages.removeIf(existing -> RouteCanonicalizer.routeEqualsOrSuffix(existing.targetRoute(), page.targetRoute()));
        pages.add(page);
    }

    private String pageId(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path == null || path.isBlank() || "/".equals(path)) return "home-page";
            String normalized = path.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-+|-+$", "");
            return normalized.isBlank() ? "page" : normalized.toLowerCase(Locale.ROOT);
        } catch (RuntimeException ignored) {
            return "page";
        }
    }

    private static PageSnapshotCollector defaultPageCollector() {
        InteractiveElementExtractor elements = new InteractiveElementExtractor();
        return new PageSnapshotCollector(elements, new FormStructureExtractor(elements));
    }

    private boolean isSafeStateAction(String intent) {
        return Set.of("OPEN_MENU", "CLICK", "LOGOUT", "SUBMIT_FORM")
                .contains(intent == null ? "" : intent.toUpperCase(Locale.ROOT));
    }

    private boolean isAuthenticationPage(ProjectProfile profile, UiInteractionPage page) {
        return profile != null && page != null
                && RouteCanonicalizer.routeEqualsOrSuffix(page.route(), profile.loginRoute())
                && !profile.authenticatedRoute().isBlank();
    }

    private void refreshFailedLocators(
            WebDriver driver,
            Map<String, TargetedLocatorVerification> locators,
            SpaInventoryConfig config
    ) {
        new ArrayList<>(locators.entrySet()).forEach(entry -> {
            if (!entry.getValue().verified()) {
                entry.setValue(verifyLocator(driver, entry.getValue(), config));
            }
        });
    }

    private TargetedLocatorVerification verifyLocator(WebDriver driver, TargetedLocatorVerification item,
                                                       SpaInventoryConfig config) {
        return locatorVerifier.verify(driver, item, config);
    }

    private TargetedActionVerification verifyAction(WebDriver driver, ProjectProfile profile,
                                                    TargetedActionVerification planned, CandidateActionEvidence candidate,
                                                    Map<String, TargetedLocatorVerification> locators,
                                                    Map<String, Boolean> outcomes, ComponentInteractionGraph graph,
                                                    SpaInventoryConfig config, UiInteractionPage page,
                                                    Map<String, UiInteractionPage> inventoryPages) {
        if (!dependenciesSatisfied(planned, outcomes, graph)) return withActionResult(planned, false, "required component action has not passed");
        return actionExecutor.execute(new SafeActionExecutor.ActionExecutionInput(driver, profile, planned, candidate,
                locators, config, page, inventoryPages));
    }

    private CandidateActionEvidence findAction(UiInteractionPage page, String actionId) {
        return page.components().stream().flatMap(component -> component.actions().stream())
                .filter(action -> actionId.equals(action.actionId())).findFirst().orElse(null);
    }

    private List<TargetedActionVerification> ordered(List<TargetedActionVerification> values,
                                                      ComponentInteractionGraph graph, String pageId) {
        return values.stream().sorted(Comparator
                .comparingInt((TargetedActionVerification action) -> prerequisites(action, graph, pageId).size())
                .thenComparingInt(this::executionOrder)
                .thenComparing(TargetedActionVerification::actionId)).toList();
    }

    private int executionOrder(TargetedActionVerification action) {
        String intent = action == null || action.intent() == null
                ? "" : action.intent().toUpperCase(Locale.ROOT);
        return switch (intent) {
            case "TYPE", "CLEAR" -> 10;
            case "OPEN_MENU" -> 20;
            case "SUBMIT_FORM", "LOGOUT" -> 30;
            default -> 15;
        };
    }

    private boolean dependenciesSatisfied(TargetedActionVerification action, Map<String, Boolean> outcomes,
                                          ComponentInteractionGraph graph) {
        return prerequisites(action, graph, action.pageId()).stream()
                .allMatch(dependency -> dependencyResolver.passed(dependency, outcomes));
    }

    private List<ComponentActionDependency> prerequisites(TargetedActionVerification action,
                                                           ComponentInteractionGraph graph, String pageId) {
        if (graph == null) return List.of();
        return graph.dependencies().stream().filter(edge -> pageId.equals(edge.pageId())
                && action.actionId().equals(edge.dependentActionId())).toList();
    }

    private TargetedLocatorVerification withLocatorResult(TargetedLocatorVerification source, boolean verified, String reason) {
        return withLocatorResult(source, verified, reason, source.qualityScore());
    }

    private TargetedLocatorVerification withLocatorResult(TargetedLocatorVerification source, boolean verified, String reason,
                                                           double qualityScore) {
        return new TargetedLocatorVerification(source.pageId(), source.route(), source.pageFingerprintHash(), source.componentId(),
                source.locatorId(), source.elementId(), source.strategy(), source.value(), qualityScore, verified, reason,
                source.requirementIds());
    }

    private TargetedActionVerification withActionResult(TargetedActionVerification source, boolean verified, String reason) {
        return withActionResult(source, verified, reason, source.confidence());
    }

    private TargetedActionVerification withActionResult(TargetedActionVerification source, boolean verified, String reason,
                                                         double confidenceFloor) {
        return new TargetedActionVerification(source.pageId(), source.route(), source.pageFingerprintHash(), source.componentId(),
                source.actionId(), source.intent(), source.targetElementId(),
                verified ? Math.max(source.confidence(), confidenceFloor) : source.confidence(), verified, reason,
                source.requirementIds());
    }

    private String join(String baseUrl, String route) {
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        String path = route == null ? "" : route.trim();
        return path.startsWith("http://") || path.startsWith("https://") ? path : base + (path.startsWith("/") ? path : "/" + path);
    }

    private String concise(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage().replaceAll("\\s+", " ").trim();
    }
}
