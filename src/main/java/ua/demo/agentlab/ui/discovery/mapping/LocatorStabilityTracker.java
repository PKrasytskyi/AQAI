package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;

public class LocatorStabilityTracker {

    public boolean uniqueOnPage(PageLocatorModel locator, PageElementModel element) {
        if (locator == null) {
            return false;
        }
        if (locator.unique()) {
            return true;
        }
        LocatorStrategy strategy = LocatorStrategy.from(locator.strategy());
        String value = safe(locator.value()).toLowerCase();
        return strategy == LocatorStrategy.ID
                || value.contains("data-testid")
                || value.contains("data-test")
                || value.contains("data-qa")
                || !safe(element == null ? "" : element.id()).isBlank();
    }

    public boolean stableAcrossRuns(PageLocatorModel locator, PageElementModel element) {
        if (locator == null) {
            return false;
        }
        if (locator.totalRuns() > 1) {
            return locator.stableAcrossRuns();
        }
        LocatorStrategy strategy = LocatorStrategy.from(locator.strategy());
        String value = safe(locator.value()).toLowerCase();
        String reason = safe(locator.reason()).toLowerCase();
        if (value.contains("data-testid") || value.contains("data-test") || value.contains("data-qa")) {
            return true;
        }
        if (strategy == LocatorStrategy.ID || strategy == LocatorStrategy.NAME) {
            return true;
        }
        if (value.contains("aria-label") || !safe(element == null ? "" : element.ariaLabel()).isBlank()) {
            return true;
        }
        return reason.contains("stable") && !strategy.equals(LocatorStrategy.XPATH);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
