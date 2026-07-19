package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessRuleResolver;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessWaiter;

import java.time.Duration;
import java.util.Objects;

/** Validates route ownership and the configured readiness rule as one typed gate. */
public final class PageReadinessVerifier {

    private final PageReadinessRuleResolver ruleResolver;
    private final PageReadinessWaiter waiter;

    public PageReadinessVerifier(PageReadinessRuleResolver ruleResolver, PageReadinessWaiter waiter) {
        this.ruleResolver = Objects.requireNonNull(ruleResolver, "ruleResolver");
        this.waiter = Objects.requireNonNull(waiter, "waiter");
    }

    public ReadinessResult await(WebDriver driver, ProjectProfile profile, String targetUrl, String expectedRoute) {
        try {
            waitForExactRoute(driver, expectedRoute);
            var result = waiter.waitUntilReady(driver, ruleResolver.resolve(profile, null, targetUrl));
            return result.ready()
                    ? ReadinessResult.ready(expectedRoute)
                    : ReadinessResult.failed("Target page readiness failed: " + result.reason());
        } catch (RuntimeException exception) {
            return ReadinessResult.failed("Route/readiness validation failed: " + concise(exception));
        }
    }

    public boolean routeMatches(WebDriver driver, String route) {
        try {
            return driver != null && RouteCanonicalizer.routeEqualsOrSuffix(driver.getCurrentUrl(), route);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void waitForExactRoute(WebDriver driver, String route) {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(current ->
                RouteCanonicalizer.routeEqualsOrSuffix(current.getCurrentUrl(), route));
    }

    private String concise(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public record ReadinessResult(boolean ready, String route, String reason) {
        public static ReadinessResult ready(String route) {
            return new ReadinessResult(true, route == null ? "" : route, "ready");
        }

        public static ReadinessResult failed(String reason) {
            return new ReadinessResult(false, "", reason == null ? "readiness failed" : reason);
        }
    }
}
