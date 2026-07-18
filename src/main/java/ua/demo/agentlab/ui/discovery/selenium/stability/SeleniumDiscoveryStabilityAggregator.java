package ua.demo.agentlab.ui.discovery.selenium.stability;

import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationResult;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveryLocatorKey;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SeleniumDiscoveryStabilityAggregator {

    public SeleniumDiscoveryResult aggregate(List<SeleniumDiscoveryResult> runs) {
        List<SeleniumDiscoveryResult> allRuns = runs == null ? List.of() : runs.stream().filter(run -> run != null).toList();
        List<SeleniumDiscoveryResult> successfulRuns = allRuns.stream().filter(run -> !run.pages().isEmpty()).toList();
        if (successfulRuns.isEmpty()) {
            List<DiscoveryAuthenticationResult> attempts = allRuns.stream()
                    .flatMap(run -> run.authenticationResults().stream()).toList();
            List<PageReadinessResult> readiness = allRuns.stream()
                    .flatMap(run -> run.readinessResults().stream()).toList();
            String baseUrl = allRuns.isEmpty() ? "" : allRuns.get(0).baseUrl();
            return new SeleniumDiscoveryResult(baseUrl, List.of(), List.of(), Math.max(1, allRuns.size()), Map.of(), attempts, readiness);
        }
        SeleniumDiscoveryResult representative = successfulRuns.get(0);
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<DiscoveryAuthenticationResult> authenticationResults = new ArrayList<>();
        List<PageReadinessResult> readinessResults = new ArrayList<>();
        for (SeleniumDiscoveryResult run : successfulRuns) {
            authenticationResults.addAll(run.authenticationResults());
            readinessResults.addAll(run.readinessResults());
            Set<String> runKeys = new LinkedHashSet<>();
            for (DiscoveredPageSnapshot page : run.pages()) {
                collectPageKeys(page, runKeys);
            }
            for (String key : runKeys) {
                counts.merge(key, 1, Integer::sum);
            }
        }
        return new SeleniumDiscoveryResult(
                representative.baseUrl(),
                representative.pages(),
                representative.transitions(),
                successfulRuns.size(),
                counts,
                authenticationResults,
                readinessResults
        );
    }

    private void collectPageKeys(DiscoveredPageSnapshot page, Set<String> output) {
        if (page == null) {
            return;
        }
        for (RawElement rawElement : page.rawElements()) {
            collectRawElementKeys(page.pageId(), rawElement, output);
        }
        for (LocatorHint locatorHint : page.locatorHints()) {
            add(output, page.pageId(), locatorHint.recommendedStrategy(), locatorHint.recommendedValue());
        }
    }

    private void collectRawElementKeys(String pageId, RawElement rawElement, Set<String> output) {
        if (rawElement == null) {
            return;
        }
        add(output, pageId, "css", dataAttributeLocator(rawElement));
        add(output, pageId, "css", rawElement.ariaLabel().isBlank()
                ? ""
                : rawElement.tag() + "[aria-label='" + escapeCssValue(rawElement.ariaLabel()) + "']");
        add(output, pageId, "id", rawElement.id());
        add(output, pageId, "name", rawElement.name());
        add(output, pageId, "css", rawElement.href().isBlank()
                ? ""
                : rawElement.tag() + "[href='" + escapeCssValue(rawElement.href()) + "']");
        add(output, pageId, "css", rawElement.placeholder().isBlank()
                ? ""
                : rawElement.tag() + "[placeholder='" + escapeCssValue(rawElement.placeholder()) + "']");
        add(output, pageId, "css", stableClassLocator(rawElement));
        add(output, pageId, "css", submitControlLocator(rawElement));
        if (!rawElement.text().isBlank() && ("button".equals(rawElement.tag()) || "a".equals(rawElement.tag()))) {
            add(output, pageId, "xpath", "//" + rawElement.tag()
                    + "[normalize-space()='" + escapeXpathLiteral(rawElement.text()) + "']");
        }
    }

    private void add(Set<String> output, String pageId, String strategy, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        output.add(DiscoveryLocatorKey.key(pageId, strategy, value));
    }

    private String dataAttributeLocator(RawElement rawElement) {
        if (rawElement.dataTestId().isBlank()) {
            return "";
        }
        if (rawElement.attributes().containsKey("data-testid")) {
            return "[data-testid='" + escapeCssValue(rawElement.dataTestId()) + "']";
        }
        if (rawElement.attributes().containsKey("data-test")) {
            return "[data-test='" + escapeCssValue(rawElement.dataTestId()) + "']";
        }
        if (rawElement.attributes().containsKey("data-qa")) {
            return "[data-qa='" + escapeCssValue(rawElement.dataTestId()) + "']";
        }
        return "";
    }

    private String escapeCssValue(String value) {
        return safe(value).replace("\\", "\\\\").replace("'", "\\'");
    }

    private String escapeXpathLiteral(String value) {
        return safe(value).replace("'", "\\'");
    }

    private String submitControlLocator(RawElement rawElement) {
        String tag = safe(rawElement.tag()).toLowerCase(java.util.Locale.ROOT);
        String type = safe(rawElement.type()).toLowerCase(java.util.Locale.ROOT);
        if (!"submit".equals(type)) {
            return "";
        }
        if ("button".equals(tag) || "input".equals(tag)) {
            return tag + "[type='submit']";
        }
        return "";
    }

    private String stableClassLocator(RawElement rawElement) {
        String tag = safe(rawElement.tag()).toLowerCase(java.util.Locale.ROOT);
        if (tag.isBlank()) return "";
        for (String token : safe(rawElement.cssClass()).split("\\s+")) {
            String value = token.toLowerCase(java.util.Locale.ROOT);
            if (List.of("dropdown", "breadcrumb", "topbar", "dashboard", "header", "title", "menu", "logout",
                    "button", "link").stream().anyMatch(value::contains)) {
                return tag + "." + token.replaceAll("([^a-zA-Z0-9_-])", "\\\\$1");
            }
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
