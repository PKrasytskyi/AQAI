package ua.demo.agentlab.validation.smoke;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ua.demo.agentlab.core.config.ConfigReader;
import ua.demo.agentlab.core.config.PropertiesUiRuntimeConfig;
import ua.demo.agentlab.core.config.UiRuntimeConfig;
import ua.demo.agentlab.core.ui.driver.DefaultDriverFactory;
import ua.demo.agentlab.persistence.GeneratedUiSources;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LiveCapabilitySmokeService {

    private static final String ENABLED_PROPERTY = "ui.live-smoke.enabled";
    private static final String ENABLED_ENV = "UI_LIVE_SMOKE_ENABLED";

    private final LiveSmokePlanResolver planResolver;
    private final GeneratedPomRuntimeLoader pomLoader;
    private final GeneratedPomRuntimeInvoker pomInvoker;

    public LiveCapabilitySmokeService() {
        this(new LiveSmokePlanResolver(), new GeneratedPomRuntimeLoader(), new GeneratedPomRuntimeInvoker());
    }

    LiveCapabilitySmokeService(LiveSmokePlanResolver planResolver) {
        this(planResolver, new GeneratedPomRuntimeLoader(), new GeneratedPomRuntimeInvoker());
    }

    LiveCapabilitySmokeService(
            LiveSmokePlanResolver planResolver,
            GeneratedPomRuntimeLoader pomLoader,
            GeneratedPomRuntimeInvoker pomInvoker
    ) {
        this.planResolver = planResolver == null ? new LiveSmokePlanResolver() : planResolver;
        this.pomLoader = pomLoader == null ? new GeneratedPomRuntimeLoader() : pomLoader;
        this.pomInvoker = pomInvoker == null ? new GeneratedPomRuntimeInvoker() : pomInvoker;
    }

    public LiveUiSmokeResult smoke(GeneratedUiSources sources) {
        LiveSmokePlan plan = planResolver.resolve(sources);
        String baseUrl = plan.profile() == null ? ConfigReader.getBaseUrl() : plan.profile().baseUrl();
        List<LiveUiSmokeStep> steps = new ArrayList<>();
        List<GeneratedUiSmokeIssue> issues = new ArrayList<>();
        if (!isEnabled()) {
            issues.add(issue("INFO", "LIVE_SMOKE_DISABLED", "",
                    "Live browser smoke is disabled. Set ui.live-smoke.enabled=true or UI_LIVE_SMOKE_ENABLED=true to run it."));
            return result(GeneratedUiSmokeStatus.SKIPPED, "Live browser smoke skipped because it is disabled", baseUrl, steps, issues);
        }
        String username = ConfigReader.getValidUsername();
        String password = ConfigReader.getValidPassword();
        if (username.isBlank() || password.isBlank()) {
            issues.add(issue("WARNING", "LIVE_SMOKE_CREDENTIALS_PRESENT", "",
                    "Valid credentials are missing. Set TEST_VALID_USERNAME and TEST_VALID_PASSWORD."));
            return result(GeneratedUiSmokeStatus.SKIPPED, "Live browser smoke skipped because credentials are missing", baseUrl, steps, issues);
        }
        if (!plan.hasSourcePage() || !plan.hasTargetPage()) {
            issues.add(issue("BLOCKER", "LIVE_SMOKE_CAPABILITY_PAGES_PRESENT", "",
                    "Generated source and target Page Objects are required for capability-driven live smoke"));
            return result(GeneratedUiSmokeStatus.FAILED, "Live browser smoke failed before browser start", baseUrl, steps, issues);
        }

        WebDriver driver = null;
        try {
            UiRuntimeConfig runtimeConfig = new PropertiesUiRuntimeConfig();
            driver = new DefaultDriverFactory(runtimeConfig).createDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(Math.max(5L, ConfigReader.getTimeout())));
            Object sourcePage = pomLoader.load(plan.sourcePage(), driver, runtimeConfig);
            Object targetPage = pomLoader.load(plan.targetPage(), driver, runtimeConfig);

            openSourcePage(sourcePage, plan, steps);
            satisfyPreconditions(sourcePage, username, password, steps);
            validateTargetPage(wait, targetPage, plan, steps);
            executeOptionalAction(driver, wait, targetPage, plan, steps);
            validatePostcondition(wait, sourcePage, targetPage, plan, steps);

            return result(GeneratedUiSmokeStatus.PASSED,
                    "Live browser smoke passed: capability flow "
                            + routeLabel(plan.sourceRoute()) + " -> " + routeLabel(plan.targetRoute()),
                    baseUrl,
                    steps,
                    issues);
        } catch (Exception exception) {
            issues.add(issue("BLOCKER", "LIVE_SMOKE_BROWSER_FLOW", "",
                    "Live browser smoke failed: " + safeMessage(exception)));
            return result(GeneratedUiSmokeStatus.FAILED, "Live browser smoke failed with " + issues.size() + " issue(s)",
                    baseUrl, steps, issues);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private void openSourcePage(Object sourcePage, LiveSmokePlan plan, List<LiveUiSmokeStep> steps) {
        String route = plan.sourceRoute().isBlank() ? "/" : plan.sourceRoute();
        pomInvoker.open(sourcePage);
        steps.add(step("open-source-page", "PASSED", route + " via generated POM API"));
    }

    private void satisfyPreconditions(
            Object sourcePage,
            String username,
            String password,
            List<LiveUiSmokeStep> steps
    ) {
        pomInvoker.authenticate(sourcePage, username, password);
        steps.add(step("satisfy-authentication-precondition", "PASSED",
                "credentials entered and submitted through generated POM API"));
    }

    private void validateTargetPage(WebDriverWait wait, Object targetPage, LiveSmokePlan plan,
                                    List<LiveUiSmokeStep> steps) {
        if (plan.hasTargetRoute()) {
            wait.until(ignored -> pomInvoker.routeMatches(targetPage, plan.targetRoute()));
            steps.add(step("validate-target-page-route", "PASSED",
                    plan.targetRoute() + " via generated POM assertion"));
        } else {
            steps.add(step("validate-target-page-route", "SKIPPED", "target route is not configured"));
        }
    }

    private void executeOptionalAction(
            WebDriver driver,
            WebDriverWait wait,
            Object targetPage,
            LiveSmokePlan plan,
            List<LiveUiSmokeStep> steps
    ) {
        if (!pomInvoker.hasOpenUserMenu(targetPage)) {
            steps.add(step("execute-optional-action-open-user-menu", "SKIPPED", "no user menu trigger locator in generated target page"));
            return;
        }
        pomInvoker.openUserMenu(targetPage);
        wait.until(ignored -> pomInvoker.logoutVisible(targetPage));
        steps.add(step("execute-optional-action-open-user-menu", "PASSED",
                "user menu opened through generated POM API"));
        steps.add(step("validate-postcondition-logout-visible", "PASSED",
                "logout visibility checked through generated POM assertion"));
        // A composite logout() may own the menu-opening step. Reset to its precondition while
        // preserving the authenticated session, then execute that public POM method unchanged.
        driver.navigate().refresh();
        if (plan.hasTargetRoute()) {
            wait.until(ExpectedConditions.urlContains(plan.targetRoute()));
        }
        steps.add(step("reset-target-state-for-logout", "PASSED", "authenticated page refreshed with menu closed"));
        waitForDomSettled(driver);
    }

    private void validatePostcondition(WebDriverWait wait, Object sourcePage, Object targetPage,
                                       LiveSmokePlan plan, List<LiveUiSmokeStep> steps) {
        if (!pomInvoker.hasOpenUserMenu(targetPage)) {
            wait.until(ignored -> pomInvoker.logoutVisible(targetPage));
            steps.add(step("validate-postcondition-logout-visible", "PASSED",
                    "logout visibility checked through generated POM assertion"));
        }
        pomInvoker.logout(targetPage);
        steps.add(step("execute-logout", "PASSED", "logout executed through generated POM API"));
        if (!plan.postActionRoute().isBlank()) {
            wait.until(ignored -> pomInvoker.routeMatches(sourcePage, plan.postActionRoute()));
            steps.add(step("validate-postcondition-route", "PASSED",
                    plan.postActionRoute() + " via generated POM assertion"));
        }
    }

    public boolean isEnabled() {
        String systemValue = System.getProperty(ENABLED_PROPERTY);
        if (systemValue != null && !systemValue.isBlank()) {
            return Boolean.parseBoolean(systemValue.trim());
        }
        String envValue = System.getenv(ENABLED_ENV);
        if (envValue != null && !envValue.isBlank()) {
            return Boolean.parseBoolean(envValue.trim());
        }
        return Boolean.parseBoolean(frameworkProperty(ENABLED_PROPERTY));
    }

    private String frameworkProperty(String key) {
        Properties properties = new Properties();
        try (var input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("framework.properties")) {
            if (input == null) {
                return "";
            }
            properties.load(input);
            return properties.getProperty(key, "").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void waitForDomSettled(WebDriver driver) {
        try {
            Thread.sleep(200L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }


    private String routeLabel(String route) {
        return route == null || route.isBlank() ? "<no-route>" : route;
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.lines().findFirst().orElse(exception.getClass().getSimpleName()).strip();
    }

    private LiveUiSmokeStep step(String name, String status, String detail) {
        return new LiveUiSmokeStep(name, status, detail);
    }

    private GeneratedUiSmokeIssue issue(String severity, String ruleId, String filePath, String message) {
        return new GeneratedUiSmokeIssue(severity, ruleId, filePath, message);
    }

    private LiveUiSmokeResult result(
            GeneratedUiSmokeStatus status,
            String summary,
            String baseUrl,
            List<LiveUiSmokeStep> steps,
            List<GeneratedUiSmokeIssue> issues
    ) {
        String executionMode = status == GeneratedUiSmokeStatus.SKIPPED
                ? "NOT_EXECUTED"
                : "COMPILED_GENERATED_POM_API";
        return new LiveUiSmokeResult(status, summary, baseUrl, executionMode, steps, issues);
    }
}
