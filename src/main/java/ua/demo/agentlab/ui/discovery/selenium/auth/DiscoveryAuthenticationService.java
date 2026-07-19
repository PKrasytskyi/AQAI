package ua.demo.agentlab.ui.discovery.selenium.auth;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.runtime.bidi.BiDiDiscoveryConfig;
import ua.demo.agentlab.ui.discovery.runtime.bidi.BiDiSessionManager;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DiscoveryAuthenticationService {

    private final DiscoveryAuthenticationConfig config;
    private final BiDiSessionManager biDiSessionManager;
    private final LoginReadinessPreflight readinessPreflight;

    public DiscoveryAuthenticationService(DiscoveryAuthenticationConfig config) {
        this(config, null, null);
    }

    public DiscoveryAuthenticationService(DiscoveryAuthenticationConfig config, BiDiSessionManager biDiSessionManager) {
        this(config, biDiSessionManager, null);
    }

    public DiscoveryAuthenticationService(DiscoveryAuthenticationConfig config, BiDiSessionManager biDiSessionManager,
                                          LoginReadinessPreflight readinessPreflight) {
        this.config = config == null ? new DiscoveryAuthenticationConfig() : config;
        this.biDiSessionManager = biDiSessionManager == null
                ? new BiDiSessionManager(BiDiDiscoveryConfig.disabled())
                : biDiSessionManager;
        this.readinessPreflight = readinessPreflight == null ? new LoginReadinessPreflight() : readinessPreflight;
    }

    public boolean authenticateIfNeeded(WebDriver driver, ProjectProfile projectProfile, String targetUrl) {
        return authenticate(driver, projectProfile, targetUrl).success();
    }

    public DiscoveryAuthenticationResult authenticate(WebDriver driver, ProjectProfile projectProfile, String targetUrl) {
        boolean protectedTarget = projectProfile != null && isProtectedTarget(projectProfile, targetUrl);
        boolean credentialsPresent = config.hasCredentials();
        if (driver == null || projectProfile == null || !config.enabled() || !credentialsPresent) {
            return DiscoveryAuthenticationResult.skipped(
                    protectedTarget,
                    credentialsPresent,
                    targetUrl,
                    !config.enabled() ? "authentication disabled" : "credentials missing"
            );
        }
        if (!protectedTarget) {
            return DiscoveryAuthenticationResult.skipped(false, true, targetUrl, "target is not protected");
        }

        String loginUrl = toAbsoluteUrl(projectProfile.baseUrl(), projectProfile.loginRoute());
        try {
            driver.navigate().to(loginUrl);
            biDiSessionManager.start(driver, pageIdFromUrl(loginUrl), loginUrl);
            biDiSessionManager.drain(driver);
            LoginReadinessPreflightResult preflight = readinessPreflight.await(driver, config);
            if (!preflight.ready()) {
                return new DiscoveryAuthenticationResult(true, true, true, false, loginUrl, targetUrl,
                        safeCurrentUrl(driver), preflight.failureReason(), preflight);
            }
            WebElement username = firstVisible(driver, config.usernameSelector());
            WebElement password = firstVisible(driver, config.passwordSelector());
            if (username == null || password == null) {
                return new DiscoveryAuthenticationResult(
                        true,
                        true,
                        true,
                        false,
                        loginUrl,
                        targetUrl,
                        safeCurrentUrl(driver),
                        "login form fields were not visible after a successful readiness preflight",
                        preflight
                );
            }
            username.clear();
            username.sendKeys(config.username());
            password.clear();
            password.sendKeys(config.password());

            WebElement submit = firstVisible(driver, config.submitSelector());
            if (submit != null) {
                submit.click();
            } else {
                password.sendKeys(Keys.ENTER);
            }
            biDiSessionManager.drain(driver);

            boolean success = waitForAuthenticatedState(driver, projectProfile);
            biDiSessionManager.drain(driver);
            return new DiscoveryAuthenticationResult(
                    true,
                    true,
                    true,
                    success,
                    loginUrl,
                    targetUrl,
                    safeCurrentUrl(driver),
                    success ? "authenticated" : "login did not reach authenticated state",
                    preflight
            );
        } catch (Exception exception) {
            return new DiscoveryAuthenticationResult(
                    true,
                    true,
                    true,
                    false,
                    loginUrl,
                    targetUrl,
                    safeCurrentUrl(driver),
                    exception.getClass().getSimpleName() + ": " + safe(exception.getMessage()),
                    LoginReadinessPreflightResult.skipped("authentication failed before readiness preflight completed")
            );
        }
    }

    private boolean waitForAuthenticatedState(WebDriver driver, ProjectProfile projectProfile) {
        String loginPath = normalizeRoute(projectProfile.loginRoute());
        try {
            return Boolean.TRUE.equals(new WebDriverWait(driver, config.timeout()).until(webDriver -> {
                String currentPath = normalizeRoute(webDriver.getCurrentUrl());
                String pageSource = webDriver.getPageSource().toLowerCase(Locale.ROOT);
                return !currentPath.equals(loginPath)
                        || routeMatches(currentPath, projectProfile.authenticatedRoute())
                        || routeMatches(currentPath, projectProfile.securityRoute())
                        || pageSource.contains("logout")
                        || pageSource.contains("log out")
                        || pageSource.contains("sign out");
            }));
        } catch (Exception ignored) {
            return false;
        }
    }

    private WebElement firstVisible(WebDriver driver, String selectorList) {
        for (String selector : selectors(selectorList)) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                for (WebElement element : elements) {
                    if (element.isDisplayed() && element.isEnabled()) {
                        return element;
                    }
                }
            } catch (Exception ignored) {
                // Keep trying other selectors. One invalid selector must not break auth discovery.
            }
        }
        return null;
    }

    private List<String> selectors(String selectorList) {
        if (selectorList == null || selectorList.isBlank()) {
            return List.of();
        }
        return Arrays.stream(selectorList.split(","))
                .map(String::trim)
                .filter(selector -> !selector.isBlank())
                .toList();
    }

    private boolean isProtectedTarget(ProjectProfile projectProfile, String targetUrl) {
        String targetPath = normalizeRoute(targetUrl);
        String authenticatedPath = normalizeRoute(projectProfile.authenticatedRoute());
        String securityPath = normalizeRoute(projectProfile.securityRoute());
        return !targetPath.isBlank()
                && (routeMatches(targetPath, authenticatedPath) || routeMatches(targetPath, securityPath));
    }

    private String toAbsoluteUrl(String baseUrl, String route) {
        if (route == null || route.isBlank()) {
            return baseUrl;
        }
        String normalizedRoute = route.trim();
        if (normalizedRoute.startsWith("http://") || normalizedRoute.startsWith("https://")) {
            return normalizedRoute;
        }
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim();
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        if (!normalizedRoute.startsWith("/")) {
            normalizedRoute = "/" + normalizedRoute;
        }
        return normalizedBase + normalizedRoute;
    }

    private boolean routeMatches(String targetPath, String configuredRoute) {
        if (targetPath == null || configuredRoute == null || configuredRoute.isBlank()) {
            return false;
        }
        return RouteCanonicalizer.routeEqualsOrSuffix(targetPath, configuredRoute);
    }

    private String normalizeRoute(String value) {
        return RouteCanonicalizer.canonicalize(value);
    }

    private String pageIdFromUrl(String url) {
        String route = normalizeRoute(url);
        if (route.isBlank() || "/".equals(route)) {
            return "home-page";
        }
        String normalized = route.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "page" : normalized.toLowerCase(Locale.ROOT);
    }

    private String safeCurrentUrl(WebDriver driver) {
        try {
            return driver == null ? "" : safe(driver.getCurrentUrl());
        } catch (Exception exception) {
            return "";
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
