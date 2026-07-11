package ua.demo.agentlab.validation.smoke;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.core.config.ConfigReader;
import ua.demo.agentlab.core.config.PropertiesUiRuntimeConfig;
import ua.demo.agentlab.core.config.UiRuntimeConfig;
import ua.demo.agentlab.core.ui.driver.DefaultDriverFactory;
import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveCapabilitySmokeService {

    private static final String ENABLED_PROPERTY = "ui.live-smoke.enabled";
    private static final String ENABLED_ENV = "UI_LIVE_SMOKE_ENABLED";

    private final LiveSmokePlanResolver planResolver;

    public LiveCapabilitySmokeService() {
        this(new LiveSmokePlanResolver());
    }

    LiveCapabilitySmokeService(LiveSmokePlanResolver planResolver) {
        this.planResolver = planResolver == null ? new LiveSmokePlanResolver() : planResolver;
    }

    public LiveUiSmokeResult smoke(GeneratedUiSources sources) {
        LiveSmokePlan plan = planResolver.resolve(sources);
        String baseUrl = plan.profile() == null ? ConfigReader.getBaseUrl() : plan.profile().baseUrl();
        List<LiveUiSmokeStep> steps = new ArrayList<>();
        List<GeneratedUiSmokeIssue> issues = new ArrayList<>();
        if (!enabled()) {
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

            openSourcePage(driver, plan, baseUrl, steps);
            satisfyPreconditions(driver, wait, plan.sourcePage(), username, password, steps);
            validateTargetPage(wait, plan, steps);
            executeOptionalAction(driver, wait, plan.targetPage(), steps);
            validatePostcondition(wait, plan, steps);

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

    private void openSourcePage(WebDriver driver, LiveSmokePlan plan, String baseUrl, List<LiveUiSmokeStep> steps) {
        String route = plan.sourceRoute().isBlank() ? "/" : plan.sourceRoute();
        driver.get(join(baseUrl, route));
        steps.add(step("open-source-page", "PASSED", route));
    }

    private void satisfyPreconditions(
            WebDriver driver,
            WebDriverWait wait,
            GeneratedSourceFile sourcePage,
            String username,
            String password,
            List<LiveUiSmokeStep> steps
    ) {
        By usernameInput = byFromSource(sourcePage, "usernameInput", By.cssSelector("input[name='username'], input#username, input[type='email'], input[name*='user' i], input[id*='user' i]"));
        By passwordInput = byFromSource(sourcePage, "passwordInput", By.cssSelector("input[type='password'], input[name='password'], input#password, input[name*='pass' i], input[id*='pass' i]"));
        By loginButton = byFromSource(sourcePage, "loginButton", By.cssSelector("button[type='submit'], input[type='submit'], button"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(usernameInput)).clear();
        driver.findElement(usernameInput).sendKeys(username);
        steps.add(step("satisfy-precondition-enter-username", "PASSED", "username input accepted"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(passwordInput)).clear();
        driver.findElement(passwordInput).sendKeys(password);
        steps.add(step("satisfy-precondition-enter-password", "PASSED", "password input accepted"));
        wait.until(ExpectedConditions.elementToBeClickable(loginButton)).click();
        steps.add(step("satisfy-precondition-submit-authentication", "PASSED", "authentication submitted"));
    }

    private void validateTargetPage(WebDriverWait wait, LiveSmokePlan plan, List<LiveUiSmokeStep> steps) {
        if (plan.hasTargetRoute()) {
            wait.until(ExpectedConditions.urlContains(plan.targetRoute()));
            steps.add(step("validate-target-page-route", "PASSED", plan.targetRoute()));
        } else {
            steps.add(step("validate-target-page-route", "SKIPPED", "target route is not configured"));
        }
    }

    private void executeOptionalAction(
            WebDriver driver,
            WebDriverWait wait,
            GeneratedSourceFile targetPage,
            List<LiveUiSmokeStep> steps
    ) {
        Optional<By> userMenuTrigger = optionalByFromSource(targetPage, "userMenuTrigger");
        if (userMenuTrigger.isEmpty()) {
            steps.add(step("execute-optional-action-open-user-menu", "SKIPPED", "no user menu trigger locator in generated target page"));
            return;
        }
        wait.until(ExpectedConditions.elementToBeClickable(userMenuTrigger.get())).click();
        steps.add(step("execute-optional-action-open-user-menu", "PASSED", "user menu opened"));
        waitForDomSettled(driver);
    }

    private void validatePostcondition(WebDriverWait wait, LiveSmokePlan plan, List<LiveUiSmokeStep> steps) {
        Optional<By> logoutLink = optionalByFromSource(plan.targetPage(), "logoutLink")
                .or(() -> Optional.of(By.cssSelector("a[href*='logout'], a[href*='signout'], button[name*='logout' i], button[id*='logout' i]")));
        WebElement logout = wait.until(ExpectedConditions.visibilityOfElementLocated(logoutLink.get()));
        steps.add(step("validate-postcondition-logout-visible", logout.isDisplayed() ? "PASSED" : "FAILED", "logout action visibility checked"));
        logout.click();
        if (!plan.postActionRoute().isBlank()) {
            wait.until(ExpectedConditions.urlContains(plan.postActionRoute()));
            steps.add(step("validate-postcondition-route", "PASSED", plan.postActionRoute()));
        }
    }

    private Optional<By> optionalByFromSource(GeneratedSourceFile source, String fieldName) {
        if (source == null || source.content() == null || source.content().isBlank()) {
            return Optional.empty();
        }
        Pattern pattern = Pattern.compile("private\\s+final\\s+By\\s+"
                + Pattern.quote(fieldName)
                + "\\s*=\\s*By\\.(id|name|cssSelector|xpath|partialLinkText)\\(\"((?:\\\\.|[^\"])*)\"\\);");
        Matcher matcher = pattern.matcher(source.content());
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(by(matcher.group(1), unescapeJava(matcher.group(2))));
    }

    private By byFromSource(GeneratedSourceFile source, String fieldName, By fallback) {
        return optionalByFromSource(source, fieldName).orElse(fallback);
    }

    private By by(String strategy, String value) {
        return switch (strategy) {
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "cssSelector" -> By.cssSelector(value);
            case "xpath" -> By.xpath(value);
            case "partialLinkText" -> By.partialLinkText(value);
            default -> By.cssSelector(value);
        };
    }

    private boolean enabled() {
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

    private String join(String baseUrl, String route) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        String path = route == null || route.isBlank() ? "/" : route.trim();
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        return base + path;
    }

    private String unescapeJava(String value) {
        return value == null ? "" : value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
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
        return new LiveUiSmokeResult(status, summary, baseUrl, steps, issues);
    }
}
