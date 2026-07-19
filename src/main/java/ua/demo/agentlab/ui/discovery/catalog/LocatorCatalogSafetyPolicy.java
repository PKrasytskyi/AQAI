package ua.demo.agentlab.ui.discovery.catalog;

import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LocatorCatalogSafetyPolicy {

    public List<String> rejectionReasons(
            CandidateLocatorEvidence candidate,
            TargetedLocatorVerification liveVerification
    ) {
        return rejectionReasons(candidate, liveVerification, false);
    }

    public List<String> rejectionReasons(
            CandidateLocatorEvidence candidate,
            TargetedLocatorVerification liveVerification,
            boolean repeatedObservation
    ) {
        List<String> reasons = new ArrayList<>();
        if (candidate == null) {
            return List.of("candidate metadata is missing");
        }
        String value = normalize(candidate.value());
        String evidence = normalize(candidate.elementId() + " " + candidate.value() + " "
                + String.join(" ", candidate.risks()));
        if (!candidate.sameOrigin() || containsAny(value, "http://", "https://")) {
            reasons.add("external or non-same-origin locator");
        }
        if (containsAny(evidence, "hidden", "type='hidden'", "type=\"hidden\"")) {
            reasons.add("hidden element");
        }
        if (containsAny(evidence, "_token", "csrf", "xsrf", "authenticity_token")) {
            reasons.add("security token element");
        }
        if (isAbsoluteXpath(value)) {
            reasons.add("absolute XPath");
        }
        if (containsAny(evidence, "dynamic-hash", "dynamic_css", "framework-generated")
                || value.matches(".*\\.[a-z]{1,4}-[a-z0-9]{6,}.*")) {
            reasons.add("dynamic hash selector");
        }
        if (containsAny(value, "nth-child", "nth-of-type") || value.isBlank()) {
            reasons.add("unsafe positional or empty selector");
        }
        if (!candidate.uniqueOnPage() && !candidate.uniqueWithinComponent()) {
            reasons.add("locator is not page/component unique");
        }
        if (!candidate.stableAcrossRuns() && !repeatedObservation) {
            reasons.add("locator is not stable across discovery observations");
        }
        if (liveVerification == null || !liveVerification.verified()) {
            reasons.add("locator is not browser verified");
        }
        return reasons.stream().distinct().toList();
    }

    public boolean confirmed(
            CandidateLocatorEvidence candidate,
            TargetedLocatorVerification liveVerification
    ) {
        return confirmed(candidate, liveVerification, false);
    }

    public boolean confirmed(
            CandidateLocatorEvidence candidate,
            TargetedLocatorVerification liveVerification,
            boolean repeatedObservation
    ) {
        return rejectionReasons(candidate, liveVerification, repeatedObservation).isEmpty()
                && liveVerification.qualityScore() >= 0.75d;
    }

    public String selectorFamily(String strategy, String value) {
        String normalizedStrategy = normalize(strategy);
        String normalizedValue = normalize(value);
        if (containsAny(normalizedValue, "data-testid", "data-test", "data-qa")) return "test-attribute";
        if (normalizedStrategy.equals("id")) return "id";
        if (normalizedStrategy.equals("name")) return "name";
        if (containsAny(normalizedValue, "aria-label", "role=")) return "accessibility";
        if (normalizedStrategy.equals("xpath")) return "xpath";
        if (normalizedStrategy.equals("css")) return "css";
        return normalizedStrategy;
    }

    private boolean isAbsoluteXpath(String value) {
        return value.startsWith("/html") || value.startsWith("//html") || value.contains("/body/");
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
