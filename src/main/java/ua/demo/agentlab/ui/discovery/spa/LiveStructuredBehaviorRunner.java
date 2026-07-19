package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.core.config.PropertiesUiRuntimeConfig;
import ua.demo.agentlab.core.ui.driver.DefaultDriverFactory;
import ua.demo.agentlab.core.ui.driver.DriverFactory;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationConfig;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationService;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessRuleResolver;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessWaiter;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.SpaBehaviorExecutionBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaBehaviorExecutionResult;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Runs each structured SPA behavior in an isolated authenticated browser session. */
public final class LiveStructuredBehaviorRunner {
    private final DriverFactory driverFactory;
    private final DiscoveryAuthenticationService authentication;
    private final PageReadinessRuleResolver readinessResolver;
    private final PageReadinessWaiter readinessWaiter;
    private final LiveStructuredBehaviorExecutor executor;

    public LiveStructuredBehaviorRunner() {
        this(new DefaultDriverFactory(new PropertiesUiRuntimeConfig()),
                new DiscoveryAuthenticationService(new DiscoveryAuthenticationConfig()),
                new PageReadinessRuleResolver(), new PageReadinessWaiter(), new LiveStructuredBehaviorExecutor());
    }

    LiveStructuredBehaviorRunner(DriverFactory driverFactory, DiscoveryAuthenticationService authentication,
                                 PageReadinessRuleResolver readinessResolver, PageReadinessWaiter readinessWaiter,
                                 LiveStructuredBehaviorExecutor executor) {
        this.driverFactory = driverFactory;
        this.authentication = authentication;
        this.readinessResolver = readinessResolver;
        this.readinessWaiter = readinessWaiter;
        this.executor = executor;
    }

    public SpaBehaviorExecutionBundle verify(ProjectProfile profile, UiInteractionInventory inventory,
                                              List<BoundSpaBehaviorContract> bindings, SpaInventoryConfig config) {
        if (bindings == null || bindings.isEmpty()) {
            return new SpaBehaviorExecutionBundle(SpaBehaviorExecutionBundle.SCHEMA_VERSION, metadata(inventory), List.of(),
                    List.of("spa-structured-behavior:no-bindings"));
        }
        List<SpaBehaviorExecutionResult> results = new ArrayList<>();
        for (BoundSpaBehaviorContract binding : bindings) {
            results.add(verifyOne(profile, inventory, binding, config));
        }
        return new SpaBehaviorExecutionBundle(SpaBehaviorExecutionBundle.SCHEMA_VERSION, metadata(inventory), results,
                List.of("spa-structured-behavior:isolated-browser-sessions", "bindings=" + bindings.size()));
    }

    private SpaBehaviorExecutionResult verifyOne(ProjectProfile profile, UiInteractionInventory inventory,
                                                  BoundSpaBehaviorContract binding, SpaInventoryConfig config) {
        if (!binding.executable()) {
            return result(binding, "NEEDS_REVIEW", binding.reviewReasons(), List.of("spa-structured-behavior:binding-incomplete"));
        }
        if (profile == null || inventory == null || config == null || !config.liveVerificationEnabled()) {
            return result(binding, "SKIPPED", List.of("Live structured behavior verification is disabled or missing profile/inventory."), List.of());
        }
        WebDriver driver = null;
        try {
            driver = driverFactory.createDriver();
            String targetUrl = join(profile.baseUrl(), binding.route());
            if (!binding.route().equals(profile.loginRoute())) {
                var auth = authentication.authenticate(driver, profile, targetUrl);
                if (!auth.success()) return result(binding, "FAILED", List.of("Authentication failed: " + auth.reason()), List.of());
            }
            driver.navigate().to(targetUrl);
            waitForRoute(driver, binding.route());
            var readiness = readinessWaiter.waitUntilReady(driver, readinessResolver.resolve(profile, null, targetUrl));
            if (!readiness.ready()) {
                return result(binding, "FAILED", List.of("Target page readiness failed: " + readiness.reason()), List.of());
            }
            return executor.execute(driver, profile, binding, inventory, config);
        } catch (RuntimeException exception) {
            return result(binding, "FAILED", List.of("Live structured behavior browser error: " + concise(exception)), List.of());
        } finally {
            if (driver != null) driverFactory.shutdownDriver(driver);
        }
    }

    private void waitForRoute(WebDriver driver, String route) {
        if (route == null || route.isBlank()) return;
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(current -> RouteCanonicalizer.routeEqualsOrSuffix(current.getCurrentUrl(), route));
    }

    private SpaBehaviorExecutionResult result(BoundSpaBehaviorContract binding, String status, List<String> reasons, List<String> trace) {
        return new SpaBehaviorExecutionResult(binding.requirementId(), binding.capability(), binding.pageId(), binding.route(), binding.flowId(),
                status, binding.steps().stream().map(step -> step.locatorId()).distinct().toList(),
                binding.steps().stream().map(step -> step.actionId()).distinct().toList(), reasons, trace);
    }

    private ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata metadata(UiInteractionInventory inventory) {
        return inventory == null || inventory.pages().isEmpty() ? null : inventory.pages().get(0).runMetadata();
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
