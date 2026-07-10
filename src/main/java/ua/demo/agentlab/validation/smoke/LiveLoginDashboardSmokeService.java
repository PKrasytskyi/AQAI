package ua.demo.agentlab.validation.smoke;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
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

public class LiveLoginDashboardSmokeService {

    private static final String ENABLED_PROPERTY = "ui.live-smoke.enabled";
    private static final String ENABLED_ENV = "UI_LIVE_SMOKE_ENABLED";

    public LiveUiSmokeResult smoke(GeneratedUiSources sources) {
        String baseUrl = ConfigReader.getBaseUrl();
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
        GeneratedSourceFile loginPage = pageObject(sources, "LoginPage").orElse(null);
        GeneratedSourceFile dashboardPage = pageObject(sources, "DashboardPage").orElse(null);
        if (loginPage == null || dashboardPage == null) {
            issues.add(issue("BLOCKER", "LIVE_SMOKE_REQUIRED_PAGES_PRESENT", "",
                    "LoginPage and DashboardPage generated sources are required for live smoke"));
            return result(GeneratedUiSmokeStatus.FAILED, "Live browser smoke failed before browser start", baseUrl, steps, issues);
        }
        By usernameInput = byFromSource(loginPage, "usernameInput", By.name("username"));
        By passwordInput = byFromSource(loginPage, "passwordInput", By.cssSelector("input[type='password']"));
        By loginButton = byFromSource(loginPage, "loginButton", By.cssSelector("button[type='submit'], input[type='submit']"));
        By userMenuTrigger = byFromSource(dashboardPage, "userMenuTrigger", By.cssSelector("span.oxd-userdropdown-tab"));
        By logoutLink = byFromSource(dashboardPage, "logoutLink", By.cssSelector("a[href*='logout']"));
        UiRuntimeConfig runtimeConfig = new PropertiesUiRuntimeConfig();
        WebDriver driver = null;
        try {
            driver = new DefaultDriverFactory(runtimeConfig).createDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(Math.max(5L, ConfigReader.getTimeout())));
            String loginUrl = join(baseUrl, "/auth/login");
            driver.get(loginUrl);
            steps.add(step("open-login", "PASSED", "/auth/login"));
            wait.until(ExpectedConditions.visibilityOfElementLocated(usernameInput)).clear();
            driver.findElement(usernameInput).sendKeys(username);
            steps.add(step("enter-username", "PASSED", "username input accepted"));
            wait.until(ExpectedConditions.visibilityOfElementLocated(passwordInput)).clear();
            driver.findElement(passwordInput).sendKeys(password);
            steps.add(step("enter-password", "PASSED", "password input accepted"));
            wait.until(ExpectedConditions.elementToBeClickable(loginButton)).click();
            wait.until(ExpectedConditions.urlContains("/dashboard/index"));
            steps.add(step("login-submit", "PASSED", "/dashboard/index"));
            wait.until(ExpectedConditions.elementToBeClickable(userMenuTrigger)).click();
            steps.add(step("open-user-menu", "PASSED", "user menu opened"));
            WebElement logout = wait.until(ExpectedConditions.visibilityOfElementLocated(logoutLink));
            steps.add(step("logout-visible", logout.isDisplayed() ? "PASSED" : "FAILED", "logout action visibility checked"));
            logout.click();
            wait.until(ExpectedConditions.urlContains("/auth/login"));
            steps.add(step("logout-click", "PASSED", "/auth/login"));
            return result(GeneratedUiSmokeStatus.PASSED, "Live browser smoke passed: login -> dashboard -> user menu -> logout",
                    baseUrl, steps, issues);
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

    private Optional<GeneratedSourceFile> pageObject(GeneratedUiSources sources, String className) {
        if (sources == null || sources.pageObjectFiles().isEmpty()) {
            return Optional.empty();
        }
        return sources.pageObjectFiles().stream()
                .filter(file -> file != null && className.equals(file.className()))
                .findFirst();
    }

    private By byFromSource(GeneratedSourceFile source, String fieldName, By fallback) {
        if (source == null || source.content() == null || source.content().isBlank()) {
            return fallback;
        }
        Pattern pattern = Pattern.compile("private\\s+final\\s+By\\s+"
                + Pattern.quote(fieldName)
                + "\\s*=\\s*By\\.(id|name|cssSelector|xpath|partialLinkText)\\(\"((?:\\\\.|[^\"])*)\"\\);");
        Matcher matcher = pattern.matcher(source.content());
        if (!matcher.find()) {
            return fallback;
        }
        String strategy = matcher.group(1);
        String value = unescapeJava(matcher.group(2));
        return switch (strategy) {
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "cssSelector" -> By.cssSelector(value);
            case "xpath" -> By.xpath(value);
            case "partialLinkText" -> By.partialLinkText(value);
            default -> fallback;
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

    private String join(String baseUrl, String route) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/") && route.startsWith("/")) {
            return base.substring(0, base.length() - 1) + route;
        }
        if (!base.endsWith("/") && !route.startsWith("/")) {
            return base + "/" + route;
        }
        return base + route;
    }

    private String unescapeJava(String value) {
        return value == null ? "" : value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
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
