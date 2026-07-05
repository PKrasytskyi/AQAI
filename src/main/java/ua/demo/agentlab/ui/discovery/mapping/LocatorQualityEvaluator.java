package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceClassifier;

import java.util.List;
import java.util.Locale;

public class LocatorQualityEvaluator {

    private final LocatorOriginResolver originResolver;
    private final LocatorStabilityTracker stabilityTracker;
    private final LocatorRiskClassifier riskClassifier;
    private final LocatorEvidenceClassifier evidenceClassifier;

    public LocatorQualityEvaluator() {
        this(new LocatorOriginResolver(), new LocatorStabilityTracker(), new LocatorRiskClassifier(),
                new LocatorEvidenceClassifier());
    }

    public LocatorQualityEvaluator(
            LocatorOriginResolver originResolver,
            LocatorStabilityTracker stabilityTracker,
            LocatorRiskClassifier riskClassifier
    ) {
        this(originResolver, stabilityTracker, riskClassifier, new LocatorEvidenceClassifier());
    }

    public LocatorQualityEvaluator(
            LocatorOriginResolver originResolver,
            LocatorStabilityTracker stabilityTracker,
            LocatorRiskClassifier riskClassifier,
            LocatorEvidenceClassifier evidenceClassifier
    ) {
        this.originResolver = originResolver == null ? new LocatorOriginResolver() : originResolver;
        this.stabilityTracker = stabilityTracker == null ? new LocatorStabilityTracker() : stabilityTracker;
        this.riskClassifier = riskClassifier == null ? new LocatorRiskClassifier() : riskClassifier;
        this.evidenceClassifier = evidenceClassifier == null ? new LocatorEvidenceClassifier() : evidenceClassifier;
    }

    public LocatorCandidate evaluate(String pageUrl, PageElementModel element, PageLocatorModel locator) {
        LocatorStrategy strategy = LocatorStrategy.from(locator == null ? "" : locator.strategy());
        LocatorOriginResolver.LocatorOrigin origin = originResolver.resolve(pageUrl, element, locator);
        boolean uniqueOnPage = stabilityTracker.uniqueOnPage(locator, element);
        boolean stableAcrossRuns = stabilityTracker.stableAcrossRuns(locator, element);
        List<String> risks = riskClassifier.classify(locator, element, origin, uniqueOnPage, stableAcrossRuns);
        double score = score(strategy, locator, element, origin, uniqueOnPage, stableAcrossRuns, risks);
        LocatorCandidate candidate = new LocatorCandidate(
                strategy,
                locator == null ? "" : locator.value(),
                score,
                locator == null ? "" : locator.reason(),
                firstNonBlank(element == null ? "" : element.role(), element == null ? "" : element.technicalType()),
                firstNonBlank(element == null ? "" : element.ariaLabel(), element == null ? "" : element.text(), element == null ? "" : element.name()),
                element == null ? "" : element.text(),
                origin.href(),
                origin.originHost(),
                origin.sameOrigin(),
                uniqueOnPage,
                stableAcrossRuns,
                risks
        );
        return new LocatorCandidate(
                candidate.strategy(),
                candidate.value(),
                candidate.stabilityScore(),
                candidate.evidenceSource(),
                candidate.elementRole(),
                candidate.accessibleName(),
                candidate.visibleText(),
                candidate.href(),
                candidate.originHost(),
                candidate.sameOrigin(),
                candidate.uniqueOnPage(),
                candidate.stableAcrossRuns(),
                candidate.risks(),
                evidenceClassifier.classify(candidate)
        );
    }

    public boolean allowed(LocatorCandidate candidate) {
        if (candidate == null || candidate.value().isBlank() || candidate.strategy().isBlank()) {
            return false;
        }
        return !riskClassifier.forbidden(candidate.risks()) && candidate.stabilityScore() >= 0.10d;
    }

    private double score(
            LocatorStrategy strategy,
            PageLocatorModel locator,
            PageElementModel element,
            LocatorOriginResolver.LocatorOrigin origin,
            boolean uniqueOnPage,
            boolean stableAcrossRuns,
            List<String> risks
    ) {
        String value = locator == null ? "" : locator.value();
        String normalized = value.toLowerCase(Locale.ROOT);
        double base;
        if (containsAny(normalized, "data-testid", "data-test", "data-qa")) {
            base = 0.95d;
        } else if (strategy == LocatorStrategy.ID) {
            base = 0.90d;
        } else if (strategy == LocatorStrategy.NAME && isFormField(element)) {
            base = 0.80d;
        } else if (containsAny(normalized, "aria-label") || !safe(element == null ? "" : element.ariaLabel()).isBlank()
                || !safe(element == null ? "" : element.role()).isBlank()) {
            base = 0.75d;
        } else if (strategy == LocatorStrategy.CSS && normalized.contains("[type='submit']")
                && isSubmitControl(element)) {
            base = 0.78d;
        } else if (strategy == LocatorStrategy.CSS && sameOriginRouteHref(normalized, element, origin)) {
            base = 0.78d;
        } else if (strategy == LocatorStrategy.CSS && shortStableCss(normalized)) {
            base = 0.60d;
        } else if (strategy == LocatorStrategy.XPATH && risks.contains("external-link-text-xpath")) {
            base = 0.05d;
        } else if (strategy == LocatorStrategy.XPATH && risks.contains("long-absolute-xpath")) {
            base = 0.10d;
        } else if (strategy == LocatorStrategy.XPATH) {
            base = 0.35d;
        } else {
            base = locator == null ? 0.20d : Math.min(locator.score(), 0.60d);
        }

        if (!origin.sameOrigin()) {
            base = Math.min(base, strategy == LocatorStrategy.XPATH ? 0.05d : 0.15d);
        }
        if (risks.contains("semantic-locator-conflict")) {
            base = Math.min(base, 0.05d);
        }
        if (risks.contains("hidden-or-invisible-element") || risks.contains("security-token-field")) {
            base = Math.min(base, 0.05d);
        }
        if (locator != null && (locator.browserMatchCount() < 0 || locator.browserScopedMatchCount() < 0)) {
            base = Math.min(base, 0.69d);
        }
        if (risks.contains("generated-locator-token")) {
            base = Math.min(base, 0.35d);
        }
        if (risks.contains("generic-id")) {
            base = Math.min(base, 0.50d);
        }
        base += semanticMatchBonus(strategy, normalized, element);
        if (!uniqueOnPage) {
            base -= 0.05d;
        }
        if (!stableAcrossRuns) {
            base -= 0.15d;
        }
        return Math.max(0.0d, Math.min(1.0d, base));
    }

    private double semanticMatchBonus(LocatorStrategy strategy, String locatorValue, PageElementModel element) {
        if (element == null) {
            return 0.0d;
        }
        String evidence = (safe(element.technicalType()) + " "
                + safe(element.semanticType()) + " "
                + safe(element.inputType()) + " "
                + safe(element.name()) + " "
                + safe(element.id()) + " "
                + safe(element.placeholder()) + " "
                + safe(element.ariaLabel())).toLowerCase(Locale.ROOT);
        boolean stableAttributeStrategy = strategy == LocatorStrategy.ID
                || strategy == LocatorStrategy.NAME
                || strategy == LocatorStrategy.CSS && (locatorValue.contains("[name=")
                || locatorValue.contains("[id=")
                || locatorValue.contains("[aria-label=")
                || locatorValue.contains("[placeholder="));
        if (!stableAttributeStrategy) {
            return 0.0d;
        }
        if (containsAny(evidence, "password", "pass") && containsAny(locatorValue, "password", "pass")) {
            return 0.08d;
        }
        if (containsAny(evidence, "username", "user", "email", "login")
                && containsAny(locatorValue, "username", "user", "email")) {
            return 0.08d;
        }
        if (containsAny(evidence, "submit", "button") && containsAny(locatorValue, "submit", "button")) {
            return 0.04d;
        }
        return 0.0d;
    }

    private boolean shortStableCss(String value) {
        if (value.length() > 80 || value.contains(" > ") || value.chars().filter(ch -> ch == ' ').count() > 2) {
            return false;
        }
        return value.contains("#")
                || value.contains(".")
                || value.contains("[href=")
                || value.contains("[placeholder=")
                || value.contains("[type=");
    }

    private boolean isFormField(PageElementModel element) {
        String text = safe(element == null ? "" : element.technicalType()) + " "
                + safe(element == null ? "" : element.tag()) + " "
                + safe(element == null ? "" : element.inputType());
        return containsAny(text.toLowerCase(Locale.ROOT), "input", "field", "password", "email", "textarea", "select");
    }

    private boolean isSubmitControl(PageElementModel element) {
        String text = safe(element == null ? "" : element.technicalType()) + " "
                + safe(element == null ? "" : element.semanticType()) + " "
                + safe(element == null ? "" : element.tag()) + " "
                + safe(element == null ? "" : element.inputType());
        return containsAny(text.toLowerCase(Locale.ROOT), "button", "submit", "input");
    }

    private boolean sameOriginRouteHref(
            String locatorValue,
            PageElementModel element,
            LocatorOriginResolver.LocatorOrigin origin
    ) {
        String href = safe(element == null ? "" : element.href()).toLowerCase(Locale.ROOT);
        String type = safe(element == null ? "" : element.technicalType()).toLowerCase(Locale.ROOT);
        return locatorValue.contains("[href=")
                && origin != null
                && origin.sameOrigin()
                && type.contains("link")
                && (href.startsWith("/") || href.startsWith("./") || href.startsWith("../"));
    }

    private boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (text.contains(fragment)) {
                return true;
            }
        }
        return false;
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
}
