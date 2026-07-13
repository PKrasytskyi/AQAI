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
                || strategy == LocatorStrategy.NAME && isFormField(element)
                || strategy == LocatorStrategy.CSS && isSubmitControlLocator(value, element)
                || strategy == LocatorStrategy.CSS && isSameOriginHrefLocator(value, element)
                || value.contains("data-testid")
                || value.contains("data-test")
                || value.contains("data-qa")
                || !safe(element == null ? "" : element.id()).isBlank();
    }

    public boolean stableAcrossRuns(PageLocatorModel locator, PageElementModel element) {
        if (locator == null) {
            return false;
        }
        if (locator.totalRuns() < 2) {
            return false;
        }
        if (locator.stableAcrossRuns()) {
            return true;
        }
        LocatorStrategy strategy = LocatorStrategy.from(locator.strategy());
        String value = safe(locator.value()).toLowerCase();
        boolean stableAttribute = strategy == LocatorStrategy.ID
                || strategy == LocatorStrategy.NAME && isFormField(element)
                || strategy == LocatorStrategy.CSS && isSubmitControlLocator(value, element)
                || strategy == LocatorStrategy.CSS && isSameOriginHrefLocator(value, element)
                || value.contains("data-testid")
                || value.contains("data-test")
                || value.contains("data-qa")
                || value.contains("aria-label")
                || !safe(element == null ? "" : element.ariaLabel()).isBlank();
        int requiredRuns = Math.max(2, (int) Math.ceil(locator.totalRuns() * 0.66d));
        return stableAttribute && locator.observedRuns() >= requiredRuns;
    }

    private boolean isSubmitControlLocator(String value, PageElementModel element) {
        String normalizedValue = safe(value).toLowerCase();
        if (!normalizedValue.contains("[type='submit']") && !normalizedValue.contains("[type=\"submit\"]")) {
            return false;
        }
        String text = safe(element == null ? "" : element.technicalType()) + " "
                + safe(element == null ? "" : element.semanticType()) + " "
                + safe(element == null ? "" : element.tag()) + " "
                + safe(element == null ? "" : element.inputType());
        return text.toLowerCase().contains("submit")
                || text.toLowerCase().contains("button")
                || text.toLowerCase().contains("input");
    }

    private boolean isSameOriginHrefLocator(String value, PageElementModel element) {
        String normalizedValue = safe(value).toLowerCase();
        String href = safe(element == null ? "" : element.href()).toLowerCase();
        if (!normalizedValue.contains("[href=") || href.isBlank()) {
            return false;
        }
        if (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("//")) {
            return false;
        }
        return href.startsWith("/") || href.startsWith("./") || href.startsWith("../");
    }

    private boolean isFormField(PageElementModel element) {
        String text = safe(element == null ? "" : element.technicalType()) + " "
                + safe(element == null ? "" : element.tag()) + " "
                + safe(element == null ? "" : element.inputType()) + " "
                + safe(element == null ? "" : element.name());
        String normalized = text.toLowerCase();
        return normalized.contains("input")
                || normalized.contains("field")
                || normalized.contains("password")
                || normalized.contains("email")
                || normalized.contains("textarea")
                || normalized.contains("select");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
