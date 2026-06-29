package ua.demo.agentlab.ui.discovery.selenium.stability;

import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveryLocatorKey;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SeleniumDiscoveryStabilityAggregator {

    public SeleniumDiscoveryResult aggregate(List<SeleniumDiscoveryResult> runs) {
        List<SeleniumDiscoveryResult> successfulRuns = runs == null
                ? List.of()
                : runs.stream().filter(run -> run != null && !run.pages().isEmpty()).toList();
        if (successfulRuns.isEmpty()) {
            return new SeleniumDiscoveryResult("", List.of(), List.of(), 1, Map.of());
        }
        SeleniumDiscoveryResult representative = successfulRuns.get(0);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SeleniumDiscoveryResult run : successfulRuns) {
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
                counts
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
