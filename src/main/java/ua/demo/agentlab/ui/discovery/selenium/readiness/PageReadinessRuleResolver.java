package ua.demo.agentlab.ui.discovery.selenium.readiness;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.ui.catalog.ConfirmedPageCandidate;
import ua.demo.agentlab.ui.catalog.ConfirmedPageRegistry;
import ua.demo.agentlab.ui.catalog.ConfirmedPageSourceResolver;
import ua.demo.agentlab.ui.catalog.PageCapability;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;

import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

public class PageReadinessRuleResolver {

    private static final long DEFAULT_TIMEOUT_MILLIS = 8_000L;

    private final ConfirmedPageSourceResolver confirmedPageSourceResolver;
    private final Properties properties;

    public PageReadinessRuleResolver() {
        this(new ConfirmedPageSourceResolver(), loadFrameworkProperties());
    }

    public PageReadinessRuleResolver(ConfirmedPageSourceResolver confirmedPageSourceResolver, Properties properties) {
        if (confirmedPageSourceResolver == null) {
            throw new IllegalArgumentException("confirmedPageSourceResolver cannot be null");
        }
        this.confirmedPageSourceResolver = confirmedPageSourceResolver;
        this.properties = properties == null ? new Properties() : properties;
    }

    public PageReadinessRule resolve(
            ProjectProfile projectProfile,
            NormalizedRequirementBundle requirementBundle,
            String targetUrl
    ) {
        String route = RouteCanonicalizer.canonicalize(targetUrl);
        ConfirmedPageRegistry registry = confirmedPageSourceResolver.resolve(projectProfile, requirementBundle, List.of());
        PageCapability capability = registry.allPages().stream()
                .filter(candidate -> routeMatches(candidate, route, targetUrl))
                .findFirst()
                .map(ConfirmedPageCandidate::capability)
                .orElseGet(() -> inferCapability(route + " " + targetUrl));
        PageReadinessRule defaultRule = defaultRule(capability, route);
        return applyOverrides(defaultRule);
    }

    private boolean routeMatches(ConfirmedPageCandidate candidate, String route, String targetUrl) {
        return candidate != null
                && (RouteCanonicalizer.routeEqualsOrSuffix(candidate.route(), route)
                || RouteCanonicalizer.routeEqualsOrSuffix(candidate.route(), targetUrl));
    }

    private PageReadinessRule defaultRule(PageCapability capability, String route) {
        long timeoutMillis = defaultTimeoutMillis();
        return switch (capability) {
            case AUTHENTICATION -> new PageReadinessRule(
                    capability,
                    route,
                    List.of(
                            "input[name*='user' i], input[id*='user' i], input[placeholder*='user' i], input[type='email'], input[type='text']",
                            "input[type='password'], input[name*='pass' i], input[id*='pass' i], input[placeholder*='pass' i]",
                            "button[type='submit'], input[type='submit'], button, [role='button']"
                    ),
                    List.of("username", "password", "login", "sign in"),
                    timeoutMillis,
                    "default-authentication"
            );
            case REGISTRATION -> new PageReadinessRule(
                    capability,
                    route,
                    List.of("input:not([type='hidden']), select, textarea"),
                    List.of("register", "registration", "sign up", "create"),
                    timeoutMillis,
                    "default-registration"
            );
            case AUTHENTICATED_AREA, DASHBOARD -> new PageReadinessRule(
                    capability,
                    route,
                    List.of("nav, aside, header, [role='navigation'], [class*='dashboard' i], [href*='logout' i], button"),
                    List.of("dashboard", "logout", "profile", "welcome"),
                    timeoutMillis,
                    "default-authenticated-area"
            );
            case FORM -> new PageReadinessRule(
                    capability,
                    route,
                    List.of("input:not([type='hidden']), select, textarea, button[type='submit'], input[type='submit']"),
                    List.of("submit", "save", "create", "update"),
                    timeoutMillis,
                    "default-form"
            );
            case RECORD_LIST -> new PageReadinessRule(
                    capability,
                    route,
                    List.of("table, [role='table'], [role='grid'], [class*='table' i], [class*='list' i], [class*='grid' i]"),
                    List.of("search", "add", "list", "records", "results"),
                    timeoutMillis,
                    "default-record-list"
            );
            case RECORD_DETAILS -> new PageReadinessRule(
                    capability,
                    route,
                    List.of("main, article, section, form, [class*='detail' i], [class*='profile' i]"),
                    List.of("details", "profile", "edit", "save"),
                    timeoutMillis,
                    "default-record-details"
            );
            case SECURITY -> new PageReadinessRule(
                    capability,
                    route,
                    List.of("input:not([type='hidden']), button, [role='button']"),
                    List.of("security", "verification", "challenge", "otp", "mfa"),
                    timeoutMillis,
                    "default-security"
            );
            case RECOVERY -> new PageReadinessRule(
                    capability,
                    route,
                    List.of("input:not([type='hidden']), button[type='submit'], input[type='submit'], button"),
                    List.of("forgot", "reset", "recover", "password"),
                    timeoutMillis,
                    "default-recovery"
            );
            case CONTAINER -> new PageReadinessRule(
                    capability,
                    route,
                    List.of("main, section, [class*='container' i], [class*='cart' i], [class*='basket' i]"),
                    List.of("cart", "basket", "selected", "container"),
                    timeoutMillis,
                    "default-container"
            );
            case NAVIGATION -> new PageReadinessRule(
                    capability,
                    route,
                    List.of(),
                    List.of(),
                    timeoutMillis,
                    "default-navigation"
            );
            case GENERIC -> PageReadinessRule.generic(route, timeoutMillis);
        };
    }

    private PageReadinessRule applyOverrides(PageReadinessRule rule) {
        String prefix = "ui.discovery.readiness." + rule.capability().name().toLowerCase(Locale.ROOT) + ".";
        Optional<List<String>> requiredCss = readList(prefix + "required-css");
        Optional<List<String>> readyText = readList(prefix + "ready-text");
        long timeoutMillis = readLong(prefix + "timeout-ms", rule.timeoutMillis());
        if (requiredCss.isEmpty() && readyText.isEmpty() && timeoutMillis == rule.timeoutMillis()) {
            return rule;
        }
        return new PageReadinessRule(
                rule.capability(),
                rule.route(),
                requiredCss.orElse(rule.requiredCssSelectors()),
                readyText.orElse(rule.readyTextFragments()),
                timeoutMillis,
                rule.source() + "+profile-override"
        );
    }

    private Optional<List<String>> readList(String key) {
        String value = readRaw(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Arrays.stream(value.split(";"))
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList());
    }

    private long readLong(String key, long fallback) {
        String value = readRaw(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(500L, Long.parseLong(value.trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private long defaultTimeoutMillis() {
        return readLong("ui.discovery.readiness.timeout-ms",
                readLong("ui.discovery.page-settle-timeout-ms", DEFAULT_TIMEOUT_MILLIS));
    }

    private String readRaw(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }
        String envKey = key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String propertyValue = properties.getProperty(key);
        return propertyValue == null || propertyValue.isBlank() ? null : propertyValue.trim();
    }

    private PageCapability inferCapability(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "dashboard", "overview")) {
            return PageCapability.DASHBOARD;
        }
        if (containsAny(normalized, "login", "signin", "sign-in", "auth", "credential")) {
            return PageCapability.AUTHENTICATION;
        }
        if (containsAny(normalized, "secure", "authenticated", "logout")) {
            return PageCapability.AUTHENTICATED_AREA;
        }
        if (containsAny(normalized, "register", "registration", "signup", "sign-up")) {
            return PageCapability.REGISTRATION;
        }
        if (containsAny(normalized, "recover", "recovery", "forgot", "reset")) {
            return PageCapability.RECOVERY;
        }
        if (containsAny(normalized, "security", "challenge", "mfa", "otp")) {
            return PageCapability.SECURITY;
        }
        if (containsAny(normalized, "catalog", "listing", "list", "table", "search", "results")) {
            return PageCapability.RECORD_LIST;
        }
        if (containsAny(normalized, "details", "detail", "record", "profile")) {
            return PageCapability.RECORD_DETAILS;
        }
        if (containsAny(normalized, "form", "submit", "field", "input", "create", "update", "edit")) {
            return PageCapability.FORM;
        }
        return PageCapability.NAVIGATION;
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static Properties loadFrameworkProperties() {
        Properties loaded = new Properties();
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("framework.properties")) {
            if (input != null) {
                loaded.load(input);
            }
        } catch (Exception ignored) {
            // Readiness still has deterministic defaults when profile properties are unavailable.
        }
        return loaded;
    }
}
