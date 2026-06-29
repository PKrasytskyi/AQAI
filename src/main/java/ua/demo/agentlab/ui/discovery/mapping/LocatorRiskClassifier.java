package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocatorRiskClassifier {

    public List<String> classify(
            PageLocatorModel locator,
            PageElementModel element,
            LocatorOriginResolver.LocatorOrigin origin,
            boolean uniqueOnPage,
            boolean stableAcrossRuns
    ) {
        List<String> risks = new ArrayList<>();
        if (locator == null || safe(locator.value()).isBlank()) {
            risks.add("empty-locator");
            return risks;
        }
        LocatorStrategy strategy = LocatorStrategy.from(locator.strategy());
        String value = safe(locator.value());
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!origin.sameOrigin()) {
            risks.add("external-origin");
        }
        if (strategy == LocatorStrategy.XPATH && !safe(element == null ? "" : element.href()).isBlank() && !origin.sameOrigin()) {
            risks.add("external-link-text-xpath");
        }
        if (strategy == LocatorStrategy.XPATH && (normalized.startsWith("/html") || normalized.startsWith("(//") || depth(value) >= 6)) {
            risks.add("long-absolute-xpath");
        } else if (strategy == LocatorStrategy.XPATH && normalized.contains("normalize-space()")) {
            risks.add("visible-text-xpath");
        }
        if (!uniqueOnPage) {
            risks.add("not-proven-unique");
        }
        if (!stableAcrossRuns) {
            risks.add("UNSTABLE_DISCOVERY");
            risks.add("not-proven-stable");
        }
        if (strategy == LocatorStrategy.CSS && normalized.contains("http") && !origin.sameOrigin()) {
            risks.add("css-targets-external-url");
        }
        return risks.stream().distinct().toList();
    }

    public boolean forbidden(List<String> risks) {
        return risks != null && (risks.contains("external-origin") || risks.contains("external-link-text-xpath"));
    }

    private int depth(String value) {
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '/') {
                count++;
            }
        }
        return count;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
