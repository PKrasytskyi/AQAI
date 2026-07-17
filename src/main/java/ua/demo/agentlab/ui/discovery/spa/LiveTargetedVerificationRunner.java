package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.core.config.PropertiesUiRuntimeConfig;
import ua.demo.agentlab.core.ui.driver.DefaultDriverFactory;
import ua.demo.agentlab.core.ui.driver.DriverFactory;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationConfig;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationService;
import ua.demo.agentlab.ui.discovery.selenium.auth.RouteProtectionResolver;
import ua.demo.agentlab.ui.discovery.evidence.LocalPageEvidenceCaptureService;
import ua.demo.agentlab.ui.discovery.browser.BrowserCapabilityAction;
import ua.demo.agentlab.ui.discovery.browser.BrowserCapabilityAdapterRegistry;
import ua.demo.agentlab.ui.discovery.browser.BrowserCapabilityRequest;
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
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaStateGraph;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTargetPageSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateTransition;

import java.time.Duration;
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
    private final DriverFactory driverFactory;
    private final DiscoveryAuthenticationService authentication;
    private final RouteProtectionResolver routeProtectionResolver = new RouteProtectionResolver();
    private final PageReadinessRuleResolver readinessResolver;
    private final PageReadinessWaiter readinessWaiter;
    private final SpaStateSnapshotCaptureService stateSnapshots;
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
        this.targetPageCollector = targetPageCollector == null ? defaultPageCollector() : targetPageCollector;
        this.targetPageEvidence = targetPageEvidence == null ? new LocalPageEvidenceCaptureService() : targetPageEvidence;
    }

    public SpaLiveTargetedVerificationResult verify(ProjectProfile profile,
                                                     SpaInventoryBundle inventory,
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
        WebDriver driver = null;
        List<TargetedLocatorVerification> locators = new ArrayList<>();
        List<TargetedActionVerification> actions = new ArrayList<>();
        List<String> trace = new ArrayList<>(List.of("spa-live-verification:fresh-browser", "profile=" + profile.profileId()));
        List<UiStateSnapshot> states = new ArrayList<>();
        List<UiStateTransition> transitions = new ArrayList<>();
        List<LiveTargetPageSnapshot> targetPages = new ArrayList<>();
        boolean sessionAuthenticated = false;
        try {
            driver = driverFactory.createDriver();
            Map<String, SpaPageInventory> pages = inventory.pages().stream()
                    .collect(java.util.stream.Collectors.toMap(SpaPageInventory::pageId, page -> page, (left, right) -> left, LinkedHashMap::new));
            for (SpaPageInventory page : pages.values()) {
                List<TargetedLocatorVerification> pageLocators = planned.locatorVerifications().stream()
                        .filter(item -> page.pageId().equals(item.pageId())).toList();
                List<TargetedActionVerification> pageActions = planned.actionVerifications().stream()
                        .filter(item -> page.pageId().equals(item.pageId())).toList();
                if (pageLocators.isEmpty() && pageActions.isEmpty()) continue;

                String targetUrl = join(profile.baseUrl(), page.route());
                // Authenticate through the profile's confirmed authenticated route first. A module
                // route such as /recruitment/vacancies is not required to be hard-coded as a
                // protected route in the profile for the runner to validate it afterwards.
                boolean requiresAuthentication = routeProtectionResolver.isDeclaredProtected(profile, page, pageActions);
                String authTarget = requiresAuthentication
                        ? join(profile.baseUrl(), profile.authenticatedRoute()) : targetUrl;
                var auth = requiresAuthentication && !sessionAuthenticated
                        ? authentication.authenticate(driver, profile, authTarget) : null;
                if (requiresAuthentication && auth != null && !auth.success()) {
                    List<TargetedLocatorVerification> failedLocators = pageLocators.stream()
                            .map(item -> withLocatorResult(item, false, "live authentication failed: " + auth.reason())).toList();
                    List<TargetedActionVerification> failedActions = pageActions.stream()
                            .map(item -> withActionResult(item, false, "live authentication failed: " + auth.reason())).toList();
                    locators.addAll(failedLocators);
                    actions.addAll(failedActions);
                    continue;
                }
                sessionAuthenticated = sessionAuthenticated || (auth != null && auth.success());
                if (auth != null && auth.success()) {
                    for (SpaPageInventory candidatePage : pages.values()) {
                        if (!RouteCanonicalizer.routeEqualsOrSuffix(candidatePage.route(), profile.authenticatedRoute())) continue;
                        UiStateSnapshot authenticatedState = stateSnapshots.capture(driver, profile, candidatePage);
                        addState(states, authenticatedState);
                        trace.add("state-snapshot:authenticated-route=" + authenticatedState.route());
                        break;
                    }
                }
                try {
                    if (!currentRouteMatches(driver, page.route())) {
                        driver.navigate().to(targetUrl);
                        if (!requiresAuthentication && !sessionAuthenticated
                                && routeProtectionResolver.redirectedToLogin(profile, page.route(), driver.getCurrentUrl())) {
                            var challengeAuth = authentication.authenticate(driver, profile,
                                    join(profile.baseUrl(), profile.authenticatedRoute()));
                            if (!challengeAuth.success()) {
                                throw new IllegalStateException("live authentication failed after login redirect: "
                                        + challengeAuth.reason());
                            }
                            sessionAuthenticated = true;
                            driver.navigate().to(targetUrl);
                            trace.add("route-protection:live-login-redirect=" + page.route());
                        }
                        waitForExactRoute(driver, page.route());
                        trace.add("state-explorer:direct-route-fallback=" + page.route());
                    } else {
                        trace.add("state-explorer:reused-live-transition=" + page.route());
                    }
                    var readiness = readinessWaiter.waitUntilReady(driver, readinessResolver.resolve(profile, null, targetUrl));
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

                UiStateSnapshot currentState = stateSnapshots.capture(driver, profile, page);
                addState(states, currentState);

                Map<String, TargetedLocatorVerification> liveLocatorById = new LinkedHashMap<>();
                for (TargetedLocatorVerification plannedLocator : pageLocators) {
                    TargetedLocatorVerification result = verifyLocator(driver, plannedLocator, config);
                    liveLocatorById.put(result.locatorId(), result);
                    locators.add(result);
                }
                Map<String, Boolean> actionOutcome = new LinkedHashMap<>();
                for (TargetedActionVerification plannedAction : ordered(pageActions, graph, page.pageId())) {
                    CandidateActionEvidence candidate = findAction(page, plannedAction.actionId());
                    TargetedActionVerification result = verifyAction(driver, profile, plannedAction, candidate,
                            liveLocatorById, actionOutcome, graph, config, page, pages);
                    actionOutcome.put(result.actionId(), result.verified());
                    actions.add(result);
                    if (result.verified() && isSafeStateAction(result.intent())) {
                        LiveTargetPageSnapshot targetPage = captureTargetPage(driver, profile, page, result, planned, trace);
                        if (targetPage != null) addTargetPage(targetPages, targetPage);
                        String nextPageId = targetPage == null ? page.pageId() : targetPage.targetPageId();
                        UiStateSnapshot nextState = stateSnapshots.capture(driver, profile, nextPageId, planned.runMetadata());
                        addState(states, nextState);
                        transitions.add(transition(currentState, nextState, result));
                        currentState = nextState;
                    }
                }
            }
            boolean passed = locators.stream().allMatch(TargetedLocatorVerification::verified)
                    && actions.stream().allMatch(TargetedActionVerification::verified);
            return new SpaLiveTargetedVerificationResult(SpaLiveTargetedVerificationResult.SCHEMA_VERSION,
                    planned.runMetadata(), true, passed, locators, actions,
                    new SpaStateGraph(SpaStateGraph.SCHEMA_VERSION, planned.runMetadata(), states, transitions,
                            List.of("spa-state-graph:live-targeted-verification")), targetPages, trace);
        } catch (RuntimeException exception) {
            trace.add("live-browser-error=" + concise(exception));
            return new SpaLiveTargetedVerificationResult(SpaLiveTargetedVerificationResult.SCHEMA_VERSION,
                    planned.runMetadata(), true, false, locators, actions,
                    new SpaStateGraph(SpaStateGraph.SCHEMA_VERSION, planned.runMetadata(), states, transitions,
                            List.of("spa-state-graph:live-browser-error")), targetPages, trace);
        } finally {
            if (driver != null) driverFactory.shutdownDriver(driver);
        }
    }

    private boolean currentRouteMatches(WebDriver driver, String route) {
        try {
            return driver != null && RouteCanonicalizer.routeEqualsOrSuffix(driver.getCurrentUrl(), route);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void addState(List<UiStateSnapshot> states, UiStateSnapshot state) {
        if (state != null && states.stream().noneMatch(existing -> existing.stateId().equals(state.stateId()))) states.add(state);
    }

    private LiveTargetPageSnapshot captureTargetPage(WebDriver driver, ProjectProfile profile, SpaPageInventory sourcePage,
                                                     TargetedActionVerification action,
                                                     SpaTargetedVerificationResult planned, List<String> trace) {
        String actualRoute = RouteCanonicalizer.canonicalize(driver.getCurrentUrl());
        if (actualRoute.isBlank() || RouteCanonicalizer.routeEqualsOrSuffix(actualRoute, sourcePage.route())) return null;
        String targetUrl = driver.getCurrentUrl();
        var readiness = readinessWaiter.waitUntilReady(driver, readinessResolver.resolve(profile, null, targetUrl));
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
        return Set.of("OPEN_MENU", "CLICK", "LOGOUT").contains(intent == null ? "" : intent.toUpperCase(Locale.ROOT));
    }

    private UiStateTransition transition(UiStateSnapshot from, UiStateSnapshot to, TargetedActionVerification action) {
        boolean routeChanged = from != null && to != null && !from.route().equals(to.route());
        String transitionId = "transition-" + Integer.toHexString((from.stateId() + "|" + action.actionId() + "|" + to.stateId()).hashCode());
        return new UiStateTransition(transitionId, from.stateId(), to.stateId(), action.actionId(), action.intent(), action.reason(),
                routeChanged, !routeChanged && !from.stateId().equals(to.stateId()), action.confidence(), from.runMetadata(),
                List.of("spa-state-transition:live-action", "action-verified=" + action.verified()));
    }

    private TargetedLocatorVerification verifyLocator(WebDriver driver, TargetedLocatorVerification item,
                                                       SpaInventoryConfig config) {
        try {
            By by = by(item.strategy(), item.value());
            List<WebElement> matches = driver.findElements(by);
            boolean visible = matches.stream().anyMatch(WebElement::isDisplayed);
            boolean unique = matches.size() == 1;
            boolean verified = visible && unique;
            double score = verified ? Math.max(item.qualityScore(), config.minConfirmedScore()) : item.qualityScore();
            return withLocatorResult(item, verified, verified
                    ? "live browser confirmed exactly one visible candidate" : "live browser count=" + matches.size(), score);
        } catch (RuntimeException exception) {
            return withLocatorResult(item, false, "live locator lookup failed: " + concise(exception));
        }
    }

    private TargetedActionVerification verifyAction(WebDriver driver, ProjectProfile profile,
                                                    TargetedActionVerification planned, CandidateActionEvidence candidate,
                                                    Map<String, TargetedLocatorVerification> locators,
                                                    Map<String, Boolean> outcomes, ComponentInteractionGraph graph,
                                                    SpaInventoryConfig config, SpaPageInventory page,
                                                    Map<String, SpaPageInventory> inventoryPages) {
        if (!planned.verified() || candidate == null) return withActionResult(planned, false, "planned action is not eligible for live verification");
        if (!config.executeSafeActions()) return withActionResult(planned, false, "safe live actions are disabled by policy");
        if (!dependenciesSatisfied(planned, outcomes, graph)) return withActionResult(planned, false, "required component action has not passed");
        String intent = planned.intent().toUpperCase(Locale.ROOT);
        java.util.Optional<BrowserCapabilityAction> browserAction = BrowserCapabilityAction.fromIntent(intent);
        if (browserAction.isPresent() && doesNotRequireLocator(browserAction.get())) {
            var nativeResult = browserCapabilities.execute(driver, browserRequest(
                    browserAction.get(), null, candidate, profile, page));
            return withActionResult(planned, nativeResult.verified(), nativeResult.reason(), config.minConfirmedScore());
        }
        CandidateLocatorEvidence locator = candidate.requiredLocatorIds().stream()
                .map(locators::get).filter(item -> item != null && item.verified()).findFirst()
                .flatMap(item -> findLocator(item, candidate, planned)).orElse(null);
        if (locator == null) return withActionResult(planned, false, "action has no live-confirmed locator");
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
            By by = by(locator.strategy(), locator.value());
            if (browserAction.isPresent()) {
                var nativeResult = browserCapabilities.execute(driver, browserRequest(
                        browserAction.get(), locator, candidate, profile, page));
                return withActionResult(planned, nativeResult.verified(), nativeResult.reason(), config.minConfirmedScore());
            }
            if ("OPEN_MENU".equals(intent)) {
                wait.until(ExpectedConditions.elementToBeClickable(by)).click();
                return withActionResult(planned, true, "live browser opened component menu", config.minConfirmedScore());
            }
            if ("LOGOUT".equals(intent)) {
                WebElement control = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
                if (config.executeSessionEndingActions()) {
                    control.click();
                    waitForExactRoute(driver, profile.loginRoute());
                    return withActionResult(planned, true, "live browser clicked logout and reached login route", config.minConfirmedScore());
                }
                return withActionResult(planned, control.isDisplayed(), "live browser confirmed logout visible; click is disabled by policy",
                        config.minConfirmedScore());
            }
            if ("CLICK".equals(intent) && isNavigationAction(page, candidate)) {
                WebElement control = wait.until(ExpectedConditions.elementToBeClickable(by));
                if (!isSameOriginControl(driver, control)) {
                    return withActionResult(planned, false, "module navigation resolved to an external origin");
                }
                String expectedRoute = routeFromHrefSelector(locator.value());
                String sourceRoute = RouteCanonicalizer.canonicalize(driver.getCurrentUrl());
                control.click();
                waitForRouteChange(driver, sourceRoute);
                String actualRoute = RouteCanonicalizer.canonicalize(driver.getCurrentUrl());
                String targetPageId = inventoryPages.values().stream()
                        .filter(target -> RouteCanonicalizer.routeEqualsOrSuffix(target.route(), actualRoute))
                        .map(SpaPageInventory::pageId).findFirst().orElse("");
                String mapping = targetPageId.isBlank() ? "target route confirmed; targeted mapping is pending"
                        : "target route confirmed and mapped to inventory page=" + targetPageId;
                String routeEvidence = expectedRoute.isBlank()
                        ? "js-router route=" + actualRoute
                        : "href route=" + expectedRoute + ", observed route=" + actualRoute;
                return withActionResult(planned, true,
                        "live browser module navigation: " + mapping + "; " + routeEvidence,
                        config.minConfirmedScore());
            }
            if (Set.of("TYPE", "CLEAR", "CHECK", "UNCHECK", "SELECT", "READ").contains(intent)) {
                WebElement control = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
                boolean verified = verifyReversibleControlAction(control, intent);
                return withActionResult(planned, verified,
                        verified ? "live browser executed reversible " + intent + " probe and restored state"
                                : "reversible " + intent + " probe did not reach the expected control state",
                        config.minConfirmedScore());
            }
            return withActionResult(planned, false, "intent is observational-only in live targeted verification: " + intent);
        } catch (RuntimeException exception) {
            return withActionResult(planned, false, "live action failed: " + concise(exception));
        }
    }

    private boolean doesNotRequireLocator(BrowserCapabilityAction action) {
        return action == BrowserCapabilityAction.HTTP_AUTHENTICATE
                || action == BrowserCapabilityAction.ACCEPT_ALERT
                || action == BrowserCapabilityAction.DISMISS_ALERT
                || action == BrowserCapabilityAction.ENTER_ALERT_TEXT
                || action == BrowserCapabilityAction.SWITCH_WINDOW;
    }

    private BrowserCapabilityRequest browserRequest(BrowserCapabilityAction action,
                                                     CandidateLocatorEvidence locator,
                                                     CandidateActionEvidence candidate,
                                                     ProjectProfile profile,
                                                     SpaPageInventory page) {
        return new BrowserCapabilityRequest(
                action,
                locator == null ? "" : locator.strategy(),
                locator == null ? "" : locator.value(),
                boundDataValue(candidate),
                authenticationConfig.username(),
                authenticationConfig.password(),
                join(profile.baseUrl(), page.route())
        );
    }

    private String boundDataValue(CandidateActionEvidence candidate) {
        if (candidate == null) return "";
        return candidate.sourceTrace().stream()
                .filter(trace -> trace != null && trace.startsWith("data-value:"))
                .map(trace -> trace.substring("data-value:".length()).trim())
                .filter(value -> !value.isBlank())
                .findFirst().orElse("");
    }

    private boolean verifyReversibleControlAction(WebElement control, String intent) {
        if (control == null || !control.isDisplayed()) {
            return false;
        }
        if ("READ".equals(intent)) {
            return true;
        }
        if ("TYPE".equals(intent) || "CLEAR".equals(intent)) {
            String original = value(control);
            control.clear();
            if ("TYPE".equals(intent)) {
                control.sendKeys("agentlab-probe");
            }
            boolean verified = "TYPE".equals(intent)
                    ? value(control).contains("agentlab-probe")
                    : value(control).isEmpty();
            control.clear();
            if (!original.isEmpty()) {
                control.sendKeys(original);
            }
            return verified;
        }
        if ("CHECK".equals(intent) || "UNCHECK".equals(intent)) {
            boolean original = control.isSelected();
            boolean desired = "CHECK".equals(intent);
            if (original != desired) {
                control.click();
            }
            boolean verified = control.isSelected() == desired;
            if (control.isSelected() != original) {
                control.click();
            }
            return verified;
        }
        if ("SELECT".equals(intent)) {
            Select select = new Select(control);
            List<WebElement> options = select.getOptions();
            if (options.isEmpty()) {
                return false;
            }
            int originalIndex = Math.max(0, options.indexOf(select.getFirstSelectedOption()));
            int probeIndex = options.size() > 1 ? (originalIndex == 0 ? 1 : 0) : originalIndex;
            select.selectByIndex(probeIndex);
            boolean verified = select.getFirstSelectedOption().equals(options.get(probeIndex));
            select.selectByIndex(originalIndex);
            return verified;
        }
        return false;
    }

    private String value(WebElement element) {
        String value = element.getAttribute("value");
        return value == null ? "" : value;
    }

    private boolean isNavigationAction(SpaPageInventory page, CandidateActionEvidence action) {
        return page != null && action != null && page.components().stream().anyMatch(component ->
                component.componentId().equals(action.componentId())
                        && component.type() == ua.demo.agentlab.ui.discovery.component.model.ComponentType.NAVIGATION);
    }

    private String routeFromHrefSelector(String selector) {
        if (selector == null) return "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "href\\s*=\\s*['\\\"]([^'\\\"#][^'\\\"]*)['\\\"]", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(selector);
        if (!matcher.find()) return "";
        String href = matcher.group(1).trim();
        if (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("//")) return "";
        return href.startsWith("/") ? href : "/" + href;
    }

    private boolean isSameOriginControl(WebDriver driver, WebElement control) {
        try {
            String currentUrl = driver.getCurrentUrl();
            String href = control.getAttribute("href");
            if (href == null || href.isBlank() || "#".equals(href.trim())) return true;
            URI current = URI.create(currentUrl);
            URI target = current.resolve(href.trim());
            return target.getHost() == null || current.getHost() != null
                    && current.getHost().equalsIgnoreCase(target.getHost())
                    && normalizedPort(current) == normalizedPort(target);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private int normalizedPort(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private void waitForRouteChange(WebDriver driver, String sourceRoute) {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(current -> {
            String route = RouteCanonicalizer.canonicalize(current.getCurrentUrl());
            return !route.isBlank() && !route.equalsIgnoreCase(sourceRoute);
        });
    }

    private java.util.Optional<CandidateLocatorEvidence> findLocator(TargetedLocatorVerification verification,
                                                                       CandidateActionEvidence candidate,
                                                                       TargetedActionVerification action) {
        return java.util.Optional.of(new CandidateLocatorEvidence(verification.locatorId(), action.componentId(), verification.elementId(),
                verification.strategy(), verification.value(), verification.qualityScore(), true, 1, 1, true, true, true,
                ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType.CONFIRMED_LOCATOR,
                ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus.CANDIDATE, List.of()));
    }

    private CandidateActionEvidence findAction(SpaPageInventory page, String actionId) {
        return page.components().stream().flatMap(component -> component.actions().stream())
                .filter(action -> actionId.equals(action.actionId())).findFirst().orElse(null);
    }

    private List<TargetedActionVerification> ordered(List<TargetedActionVerification> values,
                                                      ComponentInteractionGraph graph, String pageId) {
        return values.stream().sorted(Comparator.comparingInt(action -> prerequisites(action, graph, pageId).size())).toList();
    }

    private boolean dependenciesSatisfied(TargetedActionVerification action, Map<String, Boolean> outcomes,
                                          ComponentInteractionGraph graph) {
        return prerequisites(action, graph, action.pageId()).stream()
                .allMatch(dependency -> Boolean.TRUE.equals(outcomes.get(dependency.prerequisiteActionId())));
    }

    private List<ComponentActionDependency> prerequisites(TargetedActionVerification action,
                                                           ComponentInteractionGraph graph, String pageId) {
        if (graph == null) return List.of();
        return graph.dependencies().stream().filter(edge -> pageId.equals(edge.pageId())
                && action.actionId().equals(edge.dependentActionId())).toList();
    }

    private void waitForExactRoute(WebDriver driver, String route) {
        String expected = route == null ? "" : route.trim();
        if (expected.isBlank()) return;
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(current ->
                RouteCanonicalizer.routeEqualsOrSuffix(current.getCurrentUrl(), expected));
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

    private By by(String strategy, String value) {
        return switch (strategy == null ? "" : strategy.toLowerCase(Locale.ROOT)) {
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "xpath" -> By.xpath(value);
            default -> By.cssSelector(value);
        };
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
