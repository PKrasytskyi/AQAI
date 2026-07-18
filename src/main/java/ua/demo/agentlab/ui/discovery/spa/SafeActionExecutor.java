package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.browser.BrowserCapabilityAction;
import ua.demo.agentlab.ui.discovery.browser.BrowserCapabilityAdapterRegistry;
import ua.demo.agentlab.ui.discovery.browser.BrowserCapabilityRequest;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationConfig;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationService;
import ua.demo.agentlab.ui.discovery.spa.model.*;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Executes one policy-approved action after dependency and locator gates have passed. */
public final class SafeActionExecutor {

    private final DiscoveryAuthenticationService authentication;
    private final BrowserCapabilityAdapterRegistry browserCapabilities;
    private final DiscoveryAuthenticationConfig authenticationConfig;
    private final LocatorRuntimeVerifier locatorVerifier;

    public SafeActionExecutor(
            DiscoveryAuthenticationService authentication,
            BrowserCapabilityAdapterRegistry browserCapabilities,
            DiscoveryAuthenticationConfig authenticationConfig,
            LocatorRuntimeVerifier locatorVerifier
    ) {
        this.authentication = authentication;
        this.browserCapabilities = browserCapabilities;
        this.authenticationConfig = authenticationConfig;
        this.locatorVerifier = locatorVerifier;
    }

    public TargetedActionVerification execute(ActionExecutionInput input) {
        TargetedActionVerification planned = input.planned();
        CandidateActionEvidence candidate = input.candidate();
        if (!planned.verified() || candidate == null) {
            return result(planned, false, "planned action is not eligible for live verification", planned.confidence());
        }
        if (!input.config().executeSafeActions()) {
            return result(planned, false, "safe live actions are disabled by policy", planned.confidence());
        }
        String intent = planned.intent().toUpperCase(Locale.ROOT);
        Optional<BrowserCapabilityAction> browserAction = BrowserCapabilityAction.fromIntent(intent);
        if (browserAction.isPresent() && doesNotRequireLocator(browserAction.get())) {
            var nativeResult = browserCapabilities.execute(input.driver(), browserRequest(
                    browserAction.get(), null, candidate, input.profile(), input.page()));
            return result(planned, nativeResult.verified(), nativeResult.reason(), input.config().minConfirmedScore());
        }
        CandidateLocatorEvidence locator = confirmedLocator(candidate, planned, input.locators()).orElse(null);
        if (locator == null) {
            return result(planned, false, "action has no live-confirmed locator", planned.confidence());
        }
        try {
            WebDriverWait wait = new WebDriverWait(input.driver(), Duration.ofSeconds(8));
            By by = locatorVerifier.toBy(locator.strategy(), locator.value());
            if (browserAction.isPresent()) {
                var nativeResult = browserCapabilities.execute(input.driver(), browserRequest(
                        browserAction.get(), locator, candidate, input.profile(), input.page()));
                return result(planned, nativeResult.verified(), nativeResult.reason(), input.config().minConfirmedScore());
            }
            if ("SUBMIT_FORM".equals(intent) && isAuthenticationPage(input.profile(), input.page())) {
                var authenticationResult = authentication.authenticate(input.driver(), input.profile(),
                        join(input.profile().baseUrl(), input.profile().authenticatedRoute()));
                return result(planned, authenticationResult.success(), authenticationResult.success()
                                ? "live browser executed requirement-owned authentication sequence and reached authenticated route"
                                : "live authentication sequence failed: " + authenticationResult.reason(),
                        input.config().minConfirmedScore());
            }
            if ("OPEN_MENU".equals(intent)) {
                wait.until(ExpectedConditions.elementToBeClickable(by)).click();
                return result(planned, true, "live browser opened component menu", input.config().minConfirmedScore());
            }
            if ("LOGOUT".equals(intent)) {
                WebElement control = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
                if (input.config().executeSessionEndingActions()) {
                    control.click();
                    waitForExactRoute(input.driver(), input.profile().loginRoute());
                    return result(planned, true, "live browser clicked logout and reached login route",
                            input.config().minConfirmedScore());
                }
                return result(planned, control.isDisplayed(),
                        "live browser confirmed logout visible; click is disabled by policy",
                        input.config().minConfirmedScore());
            }
            if ("CLICK".equals(intent) && isNavigationAction(input.page(), candidate)) {
                return executeNavigation(input, candidate, locator, wait);
            }
            if (Set.of("TYPE", "CLEAR", "CHECK", "UNCHECK", "SELECT", "READ").contains(intent)) {
                WebElement control = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
                boolean verified = verifyReversibleControlAction(control, intent);
                return result(planned, verified, verified
                                ? "live browser executed reversible " + intent + " probe and restored state"
                                : "reversible " + intent + " probe did not reach the expected control state",
                        input.config().minConfirmedScore());
            }
            return result(planned, false, "intent is observational-only in live targeted verification: " + intent,
                    planned.confidence());
        } catch (RuntimeException exception) {
            return result(planned, false, "live action failed: " + concise(exception), planned.confidence());
        }
    }

    private TargetedActionVerification executeNavigation(
            ActionExecutionInput input,
            CandidateActionEvidence candidate,
            CandidateLocatorEvidence locator,
            WebDriverWait wait
    ) {
        WebElement control = wait.until(ExpectedConditions.elementToBeClickable(
                locatorVerifier.toBy(locator.strategy(), locator.value())));
        if (!isSameOriginControl(input.driver(), control)) {
            return result(input.planned(), false, "module navigation resolved to an external origin",
                    input.planned().confidence());
        }
        String expectedRoute = routeFromHrefSelector(locator.value());
        String sourceRoute = RouteCanonicalizer.canonicalize(input.driver().getCurrentUrl());
        control.click();
        waitForRouteChange(input.driver(), sourceRoute);
        String actualRoute = RouteCanonicalizer.canonicalize(input.driver().getCurrentUrl());
        String targetPageId = input.inventoryPages().values().stream()
                .filter(target -> RouteCanonicalizer.routeEqualsOrSuffix(target.route(), actualRoute))
                .map(SpaPageInventory::pageId).findFirst().orElse("");
        String mapping = targetPageId.isBlank() ? "target route confirmed; targeted mapping is pending"
                : "target route confirmed and mapped to inventory page=" + targetPageId;
        String routeEvidence = expectedRoute.isBlank() ? "js-router route=" + actualRoute
                : "href route=" + expectedRoute + ", observed route=" + actualRoute;
        return result(input.planned(), true, "live browser module navigation: " + mapping + "; " + routeEvidence,
                input.config().minConfirmedScore());
    }

    private Optional<CandidateLocatorEvidence> confirmedLocator(
            CandidateActionEvidence candidate,
            TargetedActionVerification action,
            Map<String, TargetedLocatorVerification> locators
    ) {
        return candidate.requiredLocatorIds().stream()
                .map(locators::get)
                .filter(item -> item != null && item.verified())
                .findFirst()
                .map(verification -> new CandidateLocatorEvidence(verification.locatorId(), action.componentId(),
                        verification.elementId(), verification.strategy(), verification.value(), verification.qualityScore(),
                        true, 1, 1, true, true, true, LocatorEvidenceType.CONFIRMED_LOCATOR,
                        SpaEvidenceStatus.CONFIRMED, List.of("live-browser-verified")));
    }

    private boolean doesNotRequireLocator(BrowserCapabilityAction action) {
        return action == BrowserCapabilityAction.HTTP_AUTHENTICATE
                || action == BrowserCapabilityAction.ACCEPT_ALERT
                || action == BrowserCapabilityAction.DISMISS_ALERT
                || action == BrowserCapabilityAction.ENTER_ALERT_TEXT
                || action == BrowserCapabilityAction.SWITCH_WINDOW;
    }

    private BrowserCapabilityRequest browserRequest(BrowserCapabilityAction action, CandidateLocatorEvidence locator,
                                                     CandidateActionEvidence candidate, ProjectProfile profile,
                                                     SpaPageInventory page) {
        return new BrowserCapabilityRequest(action, locator == null ? "" : locator.strategy(),
                locator == null ? "" : locator.value(), boundDataValue(candidate), authenticationConfig.username(),
                authenticationConfig.password(), join(profile.baseUrl(), page.route()));
    }

    private String boundDataValue(CandidateActionEvidence candidate) {
        return candidate.sourceTrace().stream().filter(trace -> trace != null && trace.startsWith("data-value:"))
                .map(trace -> trace.substring("data-value:".length()).trim()).filter(value -> !value.isBlank())
                .findFirst().orElse("");
    }

    private boolean verifyReversibleControlAction(WebElement control, String intent) {
        if (control == null || !control.isDisplayed()) return false;
        if ("READ".equals(intent)) return true;
        if ("TYPE".equals(intent) || "CLEAR".equals(intent)) {
            String original = value(control);
            control.clear();
            if ("TYPE".equals(intent)) control.sendKeys("agentlab-probe");
            boolean verified = "TYPE".equals(intent) ? value(control).contains("agentlab-probe") : value(control).isEmpty();
            control.clear();
            if (!original.isEmpty()) control.sendKeys(original);
            return verified;
        }
        if ("CHECK".equals(intent) || "UNCHECK".equals(intent)) {
            boolean original = control.isSelected();
            boolean desired = "CHECK".equals(intent);
            if (original != desired) control.click();
            boolean verified = control.isSelected() == desired;
            if (control.isSelected() != original) control.click();
            return verified;
        }
        if ("SELECT".equals(intent)) {
            Select select = new Select(control);
            List<WebElement> options = select.getOptions();
            if (options.isEmpty()) return false;
            int originalIndex = Math.max(0, options.indexOf(select.getFirstSelectedOption()));
            int probeIndex = options.size() > 1 ? (originalIndex == 0 ? 1 : 0) : originalIndex;
            select.selectByIndex(probeIndex);
            boolean verified = select.getFirstSelectedOption().equals(options.get(probeIndex));
            select.selectByIndex(originalIndex);
            return verified;
        }
        return false;
    }

    private boolean isAuthenticationPage(ProjectProfile profile, SpaPageInventory page) {
        return RouteCanonicalizer.routeEqualsOrSuffix(page.route(), profile.loginRoute())
                && !profile.authenticatedRoute().isBlank();
    }

    private boolean isNavigationAction(SpaPageInventory page, CandidateActionEvidence action) {
        return page.components().stream().anyMatch(component -> component.componentId().equals(action.componentId())
                && component.type() == ua.demo.agentlab.ui.discovery.component.model.ComponentType.NAVIGATION);
    }

    private boolean isSameOriginControl(WebDriver driver, WebElement control) {
        try {
            URI current = URI.create(driver.getCurrentUrl());
            String href = control.getDomProperty("href");
            if (href == null || href.isBlank() || "#".equals(href.trim())) return true;
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

    private String routeFromHrefSelector(String selector) {
        if (selector == null) return "";
        var matcher = java.util.regex.Pattern.compile("href\\s*=\\s*['\"]([^'\"#][^'\"]*)['\"]",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(selector);
        if (!matcher.find()) return "";
        String href = matcher.group(1).trim();
        if (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("//")) return "";
        return href.startsWith("/") ? href : "/" + href;
    }

    private void waitForRouteChange(WebDriver driver, String sourceRoute) {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(current -> {
            String route = RouteCanonicalizer.canonicalize(current.getCurrentUrl());
            return !route.isBlank() && !route.equalsIgnoreCase(sourceRoute);
        });
    }

    private void waitForExactRoute(WebDriver driver, String route) {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(current ->
                RouteCanonicalizer.routeEqualsOrSuffix(current.getCurrentUrl(), route));
    }

    private TargetedActionVerification result(TargetedActionVerification source, boolean verified,
                                              String reason, double confidence) {
        return new TargetedActionVerification(source.pageId(), source.route(), source.pageFingerprintHash(),
                source.componentId(), source.actionId(), source.intent(), source.targetElementId(),
                verified ? Math.max(source.confidence(), confidence) : source.confidence(), verified, reason,
                source.requirementIds());
    }

    private String value(WebElement element) {
        String value = element.getDomProperty("value");
        return value == null ? "" : value;
    }

    private String join(String baseUrl, String route) {
        return baseUrl.replaceAll("/+$", "") + "/" + route.replaceAll("^/+", "");
    }

    private String concise(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public record ActionExecutionInput(
            WebDriver driver,
            ProjectProfile profile,
            TargetedActionVerification planned,
            CandidateActionEvidence candidate,
            Map<String, TargetedLocatorVerification> locators,
            SpaInventoryConfig config,
            SpaPageInventory page,
            Map<String, SpaPageInventory> inventoryPages
    ) {
        public ActionExecutionInput {
            locators = locators == null ? Map.of() : Map.copyOf(locators);
            inventoryPages = inventoryPages == null ? Map.of() : Map.copyOf(inventoryPages);
        }
    }
}
