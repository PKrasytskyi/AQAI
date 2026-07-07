package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceClassifier;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PageModelLocatorEvidenceSelector {

    private final LocatorEvidenceClassifier locatorEvidenceClassifier;
    private final LocatorSafetyPolicy locatorSafetyPolicy;
    private final PageOwnershipSlicer ownershipSlicer;

    public PageModelLocatorEvidenceSelector(
            LocatorEvidenceClassifier locatorEvidenceClassifier,
            LocatorSafetyPolicy locatorSafetyPolicy,
            PageOwnershipSlicer ownershipSlicer
    ) {
        this.locatorEvidenceClassifier = locatorEvidenceClassifier == null
                ? new LocatorEvidenceClassifier()
                : locatorEvidenceClassifier;
        this.locatorSafetyPolicy = locatorSafetyPolicy == null ? new LocatorSafetyPolicy() : locatorSafetyPolicy;
        this.ownershipSlicer = ownershipSlicer == null ? new PageOwnershipSlicer() : ownershipSlicer;
    }

    public List<PromptLocatorEvidence> select(AiContextPackage context, MappedPage targetPage) {
        if (context.pageModelBundle() == null || context.pageModelBundle().pages().isEmpty()) {
            return List.of();
        }
        return context.pageModelBundle().pages().stream()
                .filter(page -> pageModelMatchesTarget(page, targetPage))
                .findFirst()
                .map(page -> page.elements().stream()
                        .map(element -> selectPromptSafePageModelLocator(element, targetPage)
                                .map(locator -> toLocatorEvidence(element, locator))
                                .orElse(null))
                        .filter(locator -> locator != null)
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toList(),
                                locators -> preferTargetRouteLocators(locators, targetPage))))
                .orElse(List.of());
    }

    private java.util.Optional<PageLocatorModel> selectPromptSafePageModelLocator(PageElementModel element, MappedPage targetPage) {
        if (element == null || !element.visible() || element.locatorCandidates().isEmpty()) {
            return java.util.Optional.empty();
        }
        return element.locatorCandidates().stream()
                .filter(locator -> locatorSafetyPolicy.promptSafePageModelLocator(element, locator, targetPage))
                .max(Comparator.comparingDouble(locator -> locator.score() + pageModelStrategyBonus(locator.strategy())));
    }

    private PromptLocatorEvidence toLocatorEvidence(PageElementModel element, PageLocatorModel locator) {
        String elementName = firstNonBlank(
                elementNameFromPageModel(element, locator),
                element.semanticType(),
                element.technicalType(),
                "element"
        );
        return new PromptLocatorEvidence(
                fieldHint(elementName),
                fieldHint(elementName),
                normalizeStrategy(locator.strategy()),
                locator.value(),
                normalizeRole(firstNonBlank(element.role(), element.semanticType(), element.technicalType())),
                element.text(),
                element.href(),
                true,
                locator.score(),
                "",
                "",
                locator.browserMatchCount(),
                locator.browserScopedMatchCount(),
                locator.unique(),
                locatorEvidenceClassifier.classify(locator),
                List.of(
                        "page-model-locator:" + element.elementId(),
                        "evidenceType:" + locatorEvidenceClassifier.classify(locator)
                )
        );
    }

    private List<PromptLocatorEvidence> preferTargetRouteLocators(List<PromptLocatorEvidence> locators, MappedPage targetPage) {
        List<PromptLocatorEvidence> routeLocators = locators.stream()
                .filter(locator -> locatorMatchesTargetRoute(locator, targetPage))
                .toList();
        return routeLocators.isEmpty() ? locators.stream().limit(8).toList() : routeLocators.stream().limit(4).toList();
    }

    private boolean locatorMatchesTargetRoute(PromptLocatorEvidence locator, MappedPage targetPage) {
        String targetRoute = ownershipSlicer.route(targetPage);
        return RouteCanonicalizer.routeEqualsOrSuffix(locator.href(), targetRoute)
                || RouteCanonicalizer.routeEqualsOrSuffix(locator.value(), targetRoute);
    }

    private boolean pageModelMatchesTarget(PageModel page, MappedPage targetPage) {
        if (page == null || targetPage == null) {
            return false;
        }
        if (!page.pageId().isBlank() && page.pageId().equalsIgnoreCase(targetPage.pageId())) {
            return true;
        }
        String targetRoute = ownershipSlicer.route(targetPage);
        return RouteCanonicalizer.routeEqualsOrSuffix(page.route(), targetRoute)
                || RouteCanonicalizer.routeEqualsOrSuffix(page.url(), targetRoute)
                || RouteCanonicalizer.routeEqualsOrSuffix(page.route(), targetPage.url())
                || RouteCanonicalizer.routeEqualsOrSuffix(page.url(), targetPage.url());
    }

    private String elementNameFromPageModel(PageElementModel element, PageLocatorModel locator) {
        String value = locator.value();
        if (!element.name().isBlank() && !isSecurityTokenName(element.name())) {
            return element.name();
        }
        if (!element.id().isBlank()) {
            return element.id();
        }
        if (!element.ariaLabel().isBlank()) {
            return element.ariaLabel();
        }
        if (!element.placeholder().isBlank()) {
            return element.placeholder();
        }
        if (!element.text().isBlank()) {
            return element.text();
        }
        if (!element.href().isBlank()) {
            String route = RouteCanonicalizer.canonicalize(element.href());
            if (!route.isBlank() && !"/".equals(route)) {
                String[] parts = route.split("/");
                return parts[parts.length - 1];
            }
        }
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]+", " ").trim();
    }

    private boolean isSecurityTokenName(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("_token")
                || normalized.contains("csrf")
                || normalized.contains("xsrf")
                || normalized.contains("authenticity");
    }

    private double pageModelStrategyBonus(String strategy) {
        String normalized = normalizeStrategy(strategy);
        if (normalized.equals("id")) {
            return 0.05d;
        }
        if (normalized.equals("name")) {
            return 0.04d;
        }
        if (normalized.equals("css")) {
            return 0.03d;
        }
        if (normalized.equals("xpath")) {
            return -0.10d;
        }
        return 0.0d;
    }

    private String normalizeStrategy(String strategy) {
        String normalized = strategy == null ? "" : strategy.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (normalized.contains("password")) {
            return "password";
        }
        if (normalized.contains("input") || normalized.contains("field") || normalized.contains("text")) {
            return "input";
        }
        if (normalized.contains("button") || normalized.contains("submit")) {
            return "button";
        }
        if (normalized.contains("link")) {
            return "link";
        }
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private String fieldHint(String value) {
        return ownershipSlicer.fieldHint(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
