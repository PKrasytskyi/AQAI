package ua.demo.agentlab.ui.discovery.component;

import ua.demo.agentlab.ui.discovery.component.model.ScopedLocatorCandidate;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceClassifier;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ScopedLocatorValidationService {

    private final LocatorEvidenceClassifier evidenceClassifier = new LocatorEvidenceClassifier();

    public List<ScopedLocatorCandidate> validate(
            PageModel page,
            String componentId,
            List<PageElementModel> elements,
            Map<String, Set<String>> globalLocatorIndex
    ) {
        if (page == null || elements == null || elements.isEmpty()) {
            return List.of();
        }
        Set<String> componentElementIds = elements.stream()
                .map(PageElementModel::elementId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<ScopedLocatorCandidate> candidates = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (PageElementModel element : elements) {
            for (PageLocatorModel locator : element.locatorCandidates()) {
                String key = key(locator);
                if (key.isBlank() || !seen.add(element.elementId() + "|" + key)) {
                    continue;
                }
                Set<String> globalMatches = globalLocatorIndex.getOrDefault(key, Set.of());
                int modelGlobalCount = globalMatches.size();
                int modelScopedCount = (int) globalMatches.stream()
                        .filter(componentElementIds::contains)
                        .count();
                int globalCount = locator.browserMatchCount() >= 0 ? locator.browserMatchCount() : modelGlobalCount;
                int scopedCount = locator.browserScopedMatchCount() >= 0
                        ? locator.browserScopedMatchCount()
                        : modelScopedCount;
                candidates.add(candidate(page, componentId, element, locator, globalCount, scopedCount));
            }
        }
        return candidates.stream()
                .sorted(java.util.Comparator.comparingDouble(ScopedLocatorCandidate::finalScore).reversed())
                .limit(80)
                .toList();
    }

    private ScopedLocatorCandidate candidate(
            PageModel page,
            String componentId,
            PageElementModel element,
            PageLocatorModel locator,
            int globalCount,
            int scopedCount
    ) {
        boolean uniqueOnPage = globalCount == 1 || locator.unique();
        boolean uniqueWithinComponent = scopedCount == 1 || uniqueOnPage;
        double uniquenessScore = uniquenessScore(globalCount, scopedCount);
        double stabilityScore = locator.stableAcrossRuns() ? locator.score() : Math.max(0.0d, locator.score() - 0.15d);
        double readabilityScore = readabilityScore(locator.value());
        double semanticScore = semanticScore(locator, element);
        List<String> risks = risks(locator, element, uniqueOnPage, uniqueWithinComponent);
        double finalScore = uniquenessScore * 0.35d
                + stabilityScore * 0.30d
                + semanticScore * 0.20d
                + readabilityScore * 0.15d;
        if (!locator.stableAcrossRuns()) {
            finalScore -= 0.10d;
        }
        if (risks.contains("browser-global-count-missing") || risks.contains("browser-scoped-count-missing")) {
            finalScore = Math.min(finalScore, 0.69d);
        }
        if (risks.contains("hidden-or-invisible-element") || risks.contains("security-token-field")) {
            finalScore = Math.min(finalScore, 0.05d);
        }
        if (hasForbiddenSpaRisk(risks)) {
            finalScore = Math.min(finalScore, 0.40d);
        }
        ScopedLocatorCandidate candidate = new ScopedLocatorCandidate(
                page.pageId(),
                componentId,
                element.elementId(),
                locator.strategy(),
                locator.value(),
                globalCount,
                scopedCount,
                uniqueOnPage,
                uniqueWithinComponent,
                uniquenessScore,
                stabilityScore,
                readabilityScore,
                semanticScore,
                finalScore,
                risks,
                locator.stableAcrossRuns(),
                null
        );
        return new ScopedLocatorCandidate(
                candidate.pageId(),
                candidate.componentId(),
                candidate.elementId(),
                candidate.strategy(),
                candidate.value(),
                candidate.globalMatchCount(),
                candidate.scopedMatchCount(),
                candidate.uniqueOnPage(),
                candidate.uniqueWithinComponent(),
                candidate.uniquenessScore(),
                candidate.stabilityScore(),
                candidate.readabilityScore(),
                candidate.semanticScore(),
                candidate.finalScore(),
                candidate.risks(),
                candidate.stableAcrossRuns(),
                evidenceClassifier.classify(candidate)
        );
    }

    private double uniquenessScore(int globalCount, int scopedCount) {
        if (globalCount == 1) {
            return 1.0d;
        }
        if (scopedCount == 1) {
            return 0.85d;
        }
        if (scopedCount <= 0) {
            return 0.0d;
        }
        return Math.max(0.0d, 0.60d - scopedCount * 0.10d);
    }

    private double readabilityScore(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return 0.0d;
        }
        if (antiPattern(normalized)) {
            return 0.10d;
        }
        if (normalized.length() <= 40) {
            return 0.90d;
        }
        if (normalized.length() <= 80 && !normalized.contains(" > ")) {
            return 0.70d;
        }
        return 0.35d;
    }

    private double semanticScore(PageLocatorModel locator, PageElementModel element) {
        String value = normalize(locator.value());
        String evidence = normalize(String.join(" ",
                element.text(),
                element.id(),
                element.name(),
                element.placeholder(),
                element.ariaLabel(),
                element.semanticType(),
                element.technicalType(),
                element.role()
        ));
        if (value.isBlank() || evidence.isBlank()) {
            return 0.30d;
        }
        if (containsSharedToken(value, evidence)) {
            return 0.90d;
        }
        if (value.contains("submit") && evidence.contains("button")) {
            return 0.75d;
        }
        if (value.contains("search") && evidence.contains("search")) {
            return 0.90d;
        }
        return 0.45d;
    }

    private List<String> risks(
            PageLocatorModel locator,
            PageElementModel element,
            boolean uniqueOnPage,
            boolean uniqueWithinComponent
    ) {
        List<String> risks = new ArrayList<>();
        String value = safe(locator.value());
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!uniqueOnPage) {
            risks.add("not-globally-unique");
        }
        if (!uniqueWithinComponent) {
            risks.add("not-component-unique");
        }
        if (!locator.stableAcrossRuns()) {
            risks.add("UNSTABLE_DISCOVERY");
        }
        if (locator.browserMatchCount() < 0) {
            risks.add("browser-global-count-missing");
        }
        if (locator.browserScopedMatchCount() < 0) {
            risks.add("browser-scoped-count-missing");
        }
        if (!element.visible() || "hidden".equalsIgnoreCase(element.inputType())
                || element.attributes().containsKey("hidden")
                || "true".equalsIgnoreCase(element.attributes().get("aria-hidden"))) {
            risks.add("hidden-or-invisible-element");
        }
        if (containsAny(normalized + " " + evidence(element), "_token", "csrf", "xsrf", "authenticity_token")) {
            risks.add("security-token-field");
        }
        if (dynamicCssHash(normalized)) {
            risks.add("dynamic-css-hash");
        }
        if (normalized.contains(":nth-child") || normalized.contains("nth-of-type")) {
            risks.add("nth-child-selector");
        }
        if (normalized.startsWith("/html") || normalized.startsWith("//*[@id='root']")
                || normalized.startsWith("//*[@id=\"root\"]")) {
            risks.add("absolute-dom-path");
        }
        if (normalized.matches(".*\\b(mui|chakra|ant|css)-[a-z0-9_-]{5,}.*")) {
            risks.add("framework-generated-class");
        }
        if ("xpath".equalsIgnoreCase(locator.strategy()) && normalized.contains("normalize-space()")
                && containsAny(element.technicalType(), "LINK", "BUTTON")) {
            risks.add("text-only-duplicate-risk");
        }
        return risks.stream().distinct().toList();
    }

    private String evidence(PageElementModel element) {
        return String.join(" ",
                safe(element.elementId()),
                safe(element.technicalType()),
                safe(element.semanticType()),
                safe(element.tag()),
                safe(element.inputType()),
                safe(element.name()),
                safe(element.id()),
                safe(element.placeholder()),
                safe(element.ariaLabel()),
                safe(element.role()),
                safe(element.text()),
                String.join(" ", element.attributes().values()));
    }

    private boolean hasForbiddenSpaRisk(List<String> risks) {
        return risks.contains("dynamic-css-hash")
                || risks.contains("nth-child-selector")
                || risks.contains("absolute-dom-path")
                || risks.contains("framework-generated-class");
    }

    private boolean antiPattern(String value) {
        return dynamicCssHash(value)
                || value.contains(":nth-child")
                || value.contains("nth-of-type")
                || value.startsWith("/html")
                || value.contains(" > div > div")
                || value.matches(".*\\b(mui|chakra|ant|css)-[a-z0-9_-]{5,}.*");
    }

    private boolean dynamicCssHash(String value) {
        return value.matches(".*\\.(css|sc|jss|_)?-[a-z0-9]{5,}.*")
                || value.matches(".*\\.[a-z]+-[a-z0-9]{6,}.*");
    }

    private boolean containsSharedToken(String left, String right) {
        for (String token : left.split("[^a-z0-9]+")) {
            if (token.length() >= 3 && right.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String text, String... fragments) {
        String normalized = safe(text).toLowerCase(Locale.ROOT);
        for (String fragment : fragments) {
            if (normalized.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String key(PageLocatorModel locator) {
        if (locator == null || locator.strategy().isBlank() || locator.value().isBlank()) {
            return "";
        }
        return locator.strategy().trim().toLowerCase(Locale.ROOT) + "::" + locator.value().trim();
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
