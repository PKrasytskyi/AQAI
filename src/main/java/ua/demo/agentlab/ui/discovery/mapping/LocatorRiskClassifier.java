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
        if (hiddenOrInvisible(element)) {
            risks.add("hidden-or-invisible-element");
        }
        if (securityTokenField(locator, element)) {
            risks.add("security-token-field");
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
        if (semanticConflict(normalized, element)) {
            risks.add("semantic-locator-conflict");
        }
        if (strategy == LocatorStrategy.ID && genericId(normalized)) {
            risks.add("generic-id");
        }
        if ((strategy == LocatorStrategy.ID || strategy == LocatorStrategy.CSS) && generatedToken(normalized)) {
            risks.add("generated-locator-token");
        }
        risks.addAll(spaAntiPatternRisks(strategy, normalized));
        return risks.stream().distinct().toList();
    }

    public boolean forbidden(List<String> risks) {
        return risks != null && (risks.contains("external-origin")
                || risks.contains("external-link-text-xpath")
                || risks.contains("semantic-locator-conflict")
                || risks.contains("hidden-or-invisible-element")
                || risks.contains("security-token-field")
                || risks.contains("absolute-dom-path")
                || risks.contains("dynamic-css-hash")
                || risks.contains("framework-generated-class"));
    }

    private boolean hiddenOrInvisible(PageElementModel element) {
        if (element == null) {
            return false;
        }
        String evidence = normalize(String.join(" ",
                safe(element.tag()),
                safe(element.inputType()),
                safe(element.technicalType()),
                safe(element.semanticType())
        ));
        return !element.visible()
                || containsAny(evidence, "hidden")
                || "true".equalsIgnoreCase(safe(element.attributes().get("aria-hidden")))
                || element.attributes().containsKey("hidden");
    }

    private boolean securityTokenField(PageLocatorModel locator, PageElementModel element) {
        String evidence = normalize(String.join(" ",
                safe(locator == null ? "" : locator.value()),
                safe(element == null ? "" : element.name()),
                safe(element == null ? "" : element.id()),
                safe(element == null ? "" : element.placeholder()),
                safe(element == null ? "" : element.ariaLabel()),
                safe(element == null ? "" : element.inputType()),
                safe(element == null ? "" : element.semanticType())
        ));
        return containsAny(evidence,
                "_token",
                "csrf",
                "xsrf",
                "authenticitytoken",
                "requestverificationtoken",
                "anti forgery",
                "antiforgery",
                "nonce");
    }

    private boolean semanticConflict(String locatorValue, PageElementModel element) {
        String evidence = normalize(String.join(" ",
                safe(element == null ? "" : element.technicalType()),
                safe(element == null ? "" : element.semanticType()),
                safe(element == null ? "" : element.inputType()),
                safe(element == null ? "" : element.name()),
                safe(element == null ? "" : element.id()),
                safe(element == null ? "" : element.placeholder()),
                safe(element == null ? "" : element.ariaLabel())
        ));
        boolean passwordElement = containsAny(evidence, "password", "pass");
        boolean usernameElement = containsAny(evidence, "username", "user name", "userid", "user-id", "email", "login");
        if (passwordElement && (containsAny(locatorValue, "username", "user-name", "userid", "user-id", "email")
                || normalize(locatorValue).replaceAll("[^a-z0-9]+", "").equals("login"))) {
            return true;
        }
        return usernameElement && containsAny(locatorValue, "password", "pass");
    }

    private boolean genericId(String value) {
        String normalized = normalize(value).replaceAll("[^a-z0-9]+", "");
        return normalized.equals("app")
                || normalized.equals("root")
                || normalized.equals("main")
                || normalized.equals("content")
                || normalized.equals("container")
                || normalized.equals("login");
    }

    private boolean generatedToken(String value) {
        String normalized = normalize(value);
        return normalized.matches(".*[a-f0-9]{12,}.*")
                || normalized.matches(".*\\b(id|css)-?\\d{5,}\\b.*")
                || normalized.matches(".*(__|--)[a-z0-9]{8,}.*");
    }

    private List<String> spaAntiPatternRisks(LocatorStrategy strategy, String normalizedLocator) {
        List<String> risks = new ArrayList<>();
        if (dynamicCssHash(normalizedLocator)) {
            risks.add("dynamic-css-hash");
        }
        if (normalizedLocator.contains(":nth-child") || normalizedLocator.contains("nth-of-type")) {
            risks.add("nth-child-selector");
        }
        if (strategy == LocatorStrategy.XPATH
                && (normalizedLocator.startsWith("/html")
                || normalizedLocator.startsWith("//*[@id='root']")
                || normalizedLocator.startsWith("//*[@id=\"root\"]"))) {
            risks.add("absolute-dom-path");
        }
        if (normalizedLocator.contains(" > div > div") || normalizedLocator.contains(">div>div")) {
            risks.add("deep-dom-chain");
        }
        if (normalizedLocator.matches(".*\\b(mui|chakra|ant|css)-[a-z0-9_-]{5,}.*")) {
            risks.add("framework-generated-class");
        }
        return risks;
    }

    private boolean dynamicCssHash(String value) {
        return value.matches(".*\\.(css|sc|jss|_)?-[a-z0-9]{5,}.*")
                || value.matches(".*\\.[a-z]+-[a-z0-9]{6,}.*");
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

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
