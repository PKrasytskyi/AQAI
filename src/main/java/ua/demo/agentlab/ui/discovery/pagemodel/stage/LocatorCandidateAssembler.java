package ua.demo.agentlab.ui.discovery.pagemodel.stage;

import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Builds locator candidates from discovery facts; it does not classify element meaning. */
public final class LocatorCandidateAssembler {

    public List<PageLocatorModel> fromHint(LocatorHint hint, String id, String name, String href, String text) {
        List<PageLocatorModel> locators = new ArrayList<>();
        if (hint != null && !safe(hint.recommendedValue()).isBlank()) {
            locators.add(new PageLocatorModel(hint.recommendedStrategy(), hint.recommendedValue(),
                    locatorScore(hint.recommendedStrategy()), "discovered locator hint", false));
        }
        add(locators, "id", id, 0.90d, "stable id candidate");
        add(locators, "name", name, 0.84d, "name attribute candidate");
        if (!safe(href).isBlank() && !isAbsoluteHttpUrl(href)) {
            add(locators, "css", "a[href='" + escapeCssValue(href) + "']", 0.76d, "href candidate");
        }
        if (!safe(text).isBlank()) {
            add(locators, "xpath", "//*[normalize-space()='" + escapeXpathLiteral(text) + "']", 0.58d,
                    "visible text fallback");
        }
        return deduplicate(locators);
    }

    public List<PageLocatorModel> fromRaw(RawElement element) {
        List<PageLocatorModel> locators = new ArrayList<>();
        addRuntime(locators, element, "css", dataAttributeLocator(element), 0.95d, "stable data-test attribute");
        addRuntime(locators, element, "css", element.ariaLabel().isBlank() ? ""
                : element.tag() + "[aria-label='" + escapeCssValue(element.ariaLabel()) + "']", 0.88d,
                "aria-label attribute");
        addRuntime(locators, element, "css", roleLocator(element), 0.82d, "semantic ARIA role");
        addRuntime(locators, element, "xpath", element.attributes().getOrDefault("agentlab.field.locator.xpath", ""),
                0.78d, "label-scoped custom form control");
        addRuntime(locators, element, "id", element.id(), 0.90d, "stable id candidate");
        addRuntime(locators, element, "name", element.name(), 0.84d, "name attribute candidate");
        addRuntime(locators, element, "css", element.href().isBlank() || isAbsoluteHttpUrl(element.href()) ? ""
                : element.tag() + "[href='" + escapeCssValue(element.href()) + "']", 0.78d, "href attribute");
        addRuntime(locators, element, "css", element.placeholder().isBlank() ? ""
                : element.tag() + "[placeholder='" + escapeCssValue(element.placeholder()) + "']", 0.72d,
                "placeholder attribute");
        addRuntime(locators, element, "css", stableClassLocator(element), 0.78d, "stable semantic class");
        addRuntime(locators, element, "css", submitControlLocator(element), 0.82d, "submit control candidate");
        if (!element.text().isBlank() && ("button".equals(element.tag()) || "a".equals(element.tag())
                || headingTag(element.tag()))) {
            addRuntime(locators, element, "xpath", "//" + element.tag() + "[normalize-space()='"
                    + escapeXpathLiteral(element.text()) + "']", 0.62d, "button/link text fallback");
        }
        return deduplicate(locators);
    }

    public List<PageLocatorModel> fromSubmitHint(LocatorHint hint, String id, String name, String href, String text) {
        List<PageLocatorModel> result = new ArrayList<>(fromHint(hint, id, name, href, text));
        add(result, "css", "button[type='submit']", 0.82d, "submit control candidate");
        return deduplicate(result);
    }

    public List<PageLocatorModel> deduplicate(List<PageLocatorModel> locators) {
        return locators.stream().filter(locator -> !locator.value().isBlank())
                .filter(locator -> !containsAbsoluteHttpUrl(locator.value()))
                .collect(Collectors.toMap(locator -> locator.strategy() + "::" + locator.value(), locator -> locator,
                        (left, right) -> left.score() >= right.score() ? left : right, LinkedHashMap::new))
                .values().stream().sorted(Comparator.comparingDouble(PageLocatorModel::score).reversed()).toList();
    }

    private void addRuntime(List<PageLocatorModel> locators, RawElement element, String strategy, String value,
                            double score, String reason) {
        if (safe(value).isBlank()) return;
        String key = safe(strategy).toLowerCase(Locale.ROOT) + "::" + safe(value);
        int globalCount = element.locatorMatchCounts().getOrDefault(key, -1);
        int scopedCount = element.locatorScopedMatchCounts().getOrDefault(key, -1);
        boolean unique = scopedCount == 1 || scopedCount < 0 && globalCount == 1;
        locators.add(new PageLocatorModel(strategy, value, score, reason, unique, 1, 1, true,
                globalCount, scopedCount, element.locatorScopes().getOrDefault(key, "")));
    }

    private void add(List<PageLocatorModel> locators, String strategy, String value, double score, String reason) {
        if (!safe(value).isBlank()) locators.add(new PageLocatorModel(strategy, value, score, reason, false));
    }

    private String dataAttributeLocator(RawElement element) {
        if (element.dataTestId().isBlank()) return "";
        for (String attribute : List.of("data-testid", "data-test", "data-qa")) {
            if (element.attributes().containsKey(attribute)) {
                return "[" + attribute + "='" + escapeCssValue(element.dataTestId()) + "']";
            }
        }
        return "";
    }

    private String roleLocator(RawElement element) {
        String role = safe(element.role()).toLowerCase(Locale.ROOT);
        if (!List.of("combobox", "listbox", "table", "grid", "rowgroup", "row", "cell", "columnheader").contains(role)) {
            return "";
        }
        return element.tag() + "[role='" + escapeCssValue(role) + "']";
    }

    private String submitControlLocator(RawElement element) {
        String tag = safe(element.tag()).toLowerCase(Locale.ROOT);
        return "submit".equalsIgnoreCase(element.type()) && ("button".equals(tag) || "input".equals(tag))
                ? tag + "[type='submit']" : "";
    }

    private String stableClassLocator(RawElement element) {
        String tag = safe(element.tag()).toLowerCase(Locale.ROOT);
        if (tag.isBlank()) return "";
        for (String token : safe(element.cssClass()).split("\\s+")) {
            String value = token.toLowerCase(Locale.ROOT);
            if (List.of("dropdown", "breadcrumb", "topbar", "dashboard", "header", "title", "menu", "logout",
                    "button", "link").stream().anyMatch(value::contains)) {
                return tag + "." + token.replaceAll("([^a-zA-Z0-9_-])", "\\\\$1");
            }
        }
        return "";
    }

    private double locatorScore(String strategy) {
        return switch (safe(strategy).toLowerCase(Locale.ROOT)) {
            case "id" -> 0.90d;
            case "name" -> 0.84d;
            case "css" -> 0.76d;
            case "xpath" -> 0.58d;
            default -> 0.50d;
        };
    }

    private boolean headingTag(String tag) { return safe(tag).toLowerCase(Locale.ROOT).matches("h[1-6]"); }
    private boolean isAbsoluteHttpUrl(String value) { return safe(value).toLowerCase(Locale.ROOT).matches("https?://.*"); }
    private boolean containsAbsoluteHttpUrl(String value) { return safe(value).toLowerCase(Locale.ROOT).matches(".*https?://.*"); }
    private String escapeCssValue(String value) { return safe(value).replace("\\", "\\\\").replace("'", "\\'"); }
    private String escapeXpathLiteral(String value) { return safe(value).replace("'", "&apos;"); }
    private String safe(String value) { return value == null ? "" : value.trim(); }
}
