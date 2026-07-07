package ua.demo.agentlab.ui.discovery.selenium.collector;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RuntimeLocatorCountCollector {

    public List<RawElement> enrich(WebDriver driver, List<RawElement> elements) {
        if (driver == null || elements == null || elements.isEmpty()) {
            return elements == null ? List.of() : elements;
        }
        List<RawElement> enriched = new ArrayList<>();
        for (RawElement element : elements) {
            Map<String, Integer> globalCounts = new LinkedHashMap<>();
            Map<String, Integer> scopedCounts = new LinkedHashMap<>();
            Map<String, String> scopes = new LinkedHashMap<>();
            for (LocatorDescriptor descriptor : descriptors(element)) {
                By by = by(descriptor.strategy(), descriptor.value());
                if (by == null) {
                    continue;
                }
                String key = key(descriptor.strategy(), descriptor.value());
                List<WebElement> matches = safeFind(driver, by);
                globalCounts.put(key, matches.size());
                ScopedCount scoped = scopedCount(driver, by, matches);
                scopedCounts.put(key, scoped.count());
                scopes.put(key, scoped.scope());
            }
            enriched.add(new RawElement(
                    element.rawElementId(),
                    element.tag(),
                    element.type(),
                    element.text(),
                    element.id(),
                    element.name(),
                    element.placeholder(),
                    element.ariaLabel(),
                    element.role(),
                    element.href(),
                    element.dataTestId(),
                    element.cssClass(),
                    element.visible(),
                    element.enabled(),
                    element.required(),
                    element.attributes(),
                    globalCounts,
                    scopedCounts,
                    scopes
            ));
        }
        return enriched;
    }

    private ScopedCount scopedCount(WebDriver driver, By by, List<WebElement> matches) {
        if (matches == null || matches.isEmpty()) {
            return new ScopedCount(0, "");
        }
        WebElement representative = matches.get(0);
        WebElement scope = closestScope(driver, representative);
        if (scope == null) {
            return new ScopedCount(matches.size(), "document");
        }
        return new ScopedCount(safeFind(scope, by).size(), scopeName(scope));
    }

    private WebElement closestScope(WebDriver driver, WebElement element) {
        if (!(driver instanceof JavascriptExecutor executor) || element == null) {
            return null;
        }
        try {
            Object result = executor.executeScript(
                    "return arguments[0].closest(\"form, nav, aside, [role='navigation'], table, dialog, [role='dialog'], header, main, section, [data-testid], [data-test], [data-qa]\");",
                    element
            );
            return result instanceof WebElement webElement ? webElement : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String scopeName(WebElement scope) {
        return firstNonBlank(
                safeAttribute(scope, "data-testid"),
                safeAttribute(scope, "data-test"),
                safeAttribute(scope, "data-qa"),
                safeAttribute(scope, "aria-label"),
                safeAttribute(scope, "role"),
                safeAttribute(scope, "id"),
                scopeTag(scope)
        );
    }

    private String scopeTag(WebElement scope) {
        try {
            return scope.getTagName();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String safeAttribute(WebElement element, String attribute) {
        try {
            String value = element.getAttribute(attribute);
            return value == null ? "" : value.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private List<WebElement> safeFind(SearchContext context, By by) {
        try {
            return context.findElements(by);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<LocatorDescriptor> descriptors(RawElement element) {
        List<LocatorDescriptor> locators = new ArrayList<>();
        add(locators, "css", dataAttributeLocator(element));
        add(locators, "css", element.ariaLabel().isBlank()
                ? ""
                : element.tag() + "[aria-label='" + escapeCssValue(element.ariaLabel()) + "']");
        add(locators, "id", element.id());
        add(locators, "name", element.name());
        add(locators, "css", element.href().isBlank() || isAbsoluteHttpUrl(element.href())
                ? ""
                : element.tag() + "[href='" + escapeCssValue(element.href()) + "']");
        add(locators, "css", element.placeholder().isBlank()
                ? ""
                : element.tag() + "[placeholder='" + escapeCssValue(element.placeholder()) + "']");
        add(locators, "css", stableClassLocator(element));
        add(locators, "css", submitControlLocator(element));
        if (!element.text().isBlank()
                && ("button".equals(element.tag()) || "a".equals(element.tag()) || headingTag(element.tag()))) {
            add(locators, "xpath", "//" + element.tag() + "[normalize-space()='" + escapeXpathLiteral(element.text()) + "']");
        }
        return locators.stream()
                .filter(locator -> !locator.value().isBlank())
                .distinct()
                .toList();
    }

    private By by(String strategy, String value) {
        return switch (safe(strategy).toLowerCase(Locale.ROOT)) {
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "css" -> By.cssSelector(value);
            case "xpath" -> By.xpath(value);
            default -> null;
        };
    }

    private String dataAttributeLocator(RawElement element) {
        if (element.dataTestId().isBlank()) {
            return "";
        }
        if (element.attributes().containsKey("data-testid")) {
            return "[data-testid='" + escapeCssValue(element.dataTestId()) + "']";
        }
        if (element.attributes().containsKey("data-test")) {
            return "[data-test='" + escapeCssValue(element.dataTestId()) + "']";
        }
        if (element.attributes().containsKey("data-qa")) {
            return "[data-qa='" + escapeCssValue(element.dataTestId()) + "']";
        }
        return "";
    }

    private String submitControlLocator(RawElement element) {
        String tag = safe(element.tag()).toLowerCase(Locale.ROOT);
        String type = safe(element.type()).toLowerCase(Locale.ROOT);
        if (!"submit".equals(type)) {
            return "";
        }
        if ("button".equals(tag) || "input".equals(tag)) {
            return tag + "[type='submit']";
        }
        return "";
    }

    private String stableClassLocator(RawElement element) {
        String tag = safe(element.tag()).toLowerCase(Locale.ROOT);
        if (tag.isBlank() || safe(element.cssClass()).isBlank()) {
            return "";
        }
        for (String token : element.cssClass().split("\\s+")) {
            String normalized = token.toLowerCase(Locale.ROOT);
            if (stableSemanticClass(normalized)) {
                return tag + "." + escapeCssClass(token);
            }
        }
        return "";
    }

    private boolean stableSemanticClass(String token) {
        return token.contains("dropdown")
                || token.contains("breadcrumb")
                || token.contains("topbar")
                || token.contains("dashboard")
                || token.contains("header")
                || token.contains("title")
                || token.contains("menu")
                || token.contains("logout")
                || token.contains("button")
                || token.contains("link");
    }

    private boolean headingTag(String tag) {
        return safe(tag).toLowerCase(Locale.ROOT).matches("h[1-6]");
    }

    private String escapeCssClass(String value) {
        return safe(value).replace("\\", "\\\\").replace(".", "\\.");
    }

    private void add(List<LocatorDescriptor> locators, String strategy, String value) {
        if (value != null && !value.isBlank()) {
            locators.add(new LocatorDescriptor(strategy, value));
        }
    }

    private String key(String strategy, String value) {
        return safe(strategy).toLowerCase(Locale.ROOT) + "::" + safe(value);
    }

    private String escapeCssValue(String value) {
        return safe(value).replace("\\", "\\\\").replace("'", "\\'");
    }

    private String escapeXpathLiteral(String value) {
        return safe(value).replace("'", "\\'");
    }

    private boolean isAbsoluteHttpUrl(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record LocatorDescriptor(String strategy, String value) {
    }

    private record ScopedCount(int count, String scope) {
    }
}
