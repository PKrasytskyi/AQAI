package ua.demo.agentlab.ui.discovery.selenium.auth;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;

import java.util.List;
import java.util.Locale;

public class DiscoveryAuthenticationService {

    private final DiscoveryAuthenticationConfig config;

    public DiscoveryAuthenticationService(DiscoveryAuthenticationConfig config) {
        this.config = config == null ? new DiscoveryAuthenticationConfig() : config;
    }

    public boolean authenticateIfNeeded(WebDriver driver, ProjectProfile projectProfile, String targetUrl) {
        if (driver == null || projectProfile == null || !config.enabled() || !config.hasCredentials()) {
            return false;
        }
        if (!isProtectedTarget(projectProfile, targetUrl)) {
            return false;
        }

        try {
            driver.navigate().to(toAbsoluteUrl(projectProfile.baseUrl(), projectProfile.loginRoute()));
            WebElement username = firstVisible(driver, By.cssSelector(config.usernameSelector()));
            WebElement password = firstVisible(driver, By.cssSelector(config.passwordSelector()));
            if (username == null || password == null) {
                return false;
            }
            username.clear();
            username.sendKeys(config.username());
            password.clear();
            password.sendKeys(config.password());

            WebElement submit = firstVisible(driver, By.cssSelector(config.submitSelector()));
            if (submit != null) {
                submit.click();
            } else {
                password.submit();
            }

            waitForNavigationAwayFromLogin(driver, projectProfile);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private void waitForNavigationAwayFromLogin(WebDriver driver, ProjectProfile projectProfile) {
        String loginPath = normalizeRoute(projectProfile.loginRoute());
        try {
            new WebDriverWait(driver, config.timeout()).until(webDriver -> {
                String currentPath = normalizeRoute(webDriver.getCurrentUrl());
                return !currentPath.equals(loginPath)
                        || webDriver.getPageSource().toLowerCase(Locale.ROOT).contains("logout");
            });
        } catch (Exception ignored) {
            // Discovery should continue even when an application keeps the user on the login page.
        }
    }

    private WebElement firstVisible(WebDriver driver, By locator) {
        List<WebElement> elements = driver.findElements(locator);
        for (WebElement element : elements) {
            try {
                if (element.isDisplayed() && element.isEnabled()) {
                    return element;
                }
            } catch (Exception ignored) {
                // Keep trying other candidates.
            }
        }
        return null;
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
}
