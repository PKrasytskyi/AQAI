package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.component.ComponentBoundaryDetector;
import ua.demo.agentlab.ui.discovery.evidence.EvidenceRankingService;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceClassifier;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.semantic.SemanticActionModelBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocatorEvidenceSelector {

    private final PromptLocatorSelector locatorSelector;
    private final DbStableLocatorEvidenceSelector dbStableLocatorEvidenceSelector;
    private final ComponentLocatorEvidenceSelector componentLocatorEvidenceSelector;
    private final PageModelLocatorEvidenceSelector pageModelLocatorEvidenceSelector;

    public LocatorEvidenceSelector() {
        this(
                new PromptLocatorSelector(),
                new SemanticActionModelBuilder(),
                new ComponentBoundaryDetector(),
                new EvidenceRankingService(),
                new LocatorEvidenceClassifier(),
                new PageOwnershipSlicer()
        );
    }

    public LocatorEvidenceSelector(
            PromptLocatorSelector locatorSelector,
            SemanticActionModelBuilder semanticActionModelBuilder,
            ComponentBoundaryDetector componentBoundaryDetector,
            EvidenceRankingService evidenceRankingService,
            LocatorEvidenceClassifier locatorEvidenceClassifier,
            PageOwnershipSlicer ownershipSlicer
    ) {
        PromptLocatorSelector safeLocatorSelector = locatorSelector == null
                ? new PromptLocatorSelector()
                : locatorSelector;
        PageOwnershipSlicer safeOwnershipSlicer = ownershipSlicer == null
                ? new PageOwnershipSlicer()
                : ownershipSlicer;
        LocatorSafetyPolicy locatorSafetyPolicy = new LocatorSafetyPolicy();
        this.locatorSelector = safeLocatorSelector;
        this.dbStableLocatorEvidenceSelector = new DbStableLocatorEvidenceSelector(safeOwnershipSlicer);
        this.componentLocatorEvidenceSelector = new ComponentLocatorEvidenceSelector(
                componentBoundaryDetector == null ? new ComponentBoundaryDetector() : componentBoundaryDetector,
                semanticActionModelBuilder == null ? new SemanticActionModelBuilder() : semanticActionModelBuilder,
                evidenceRankingService == null ? new EvidenceRankingService() : evidenceRankingService,
                safeOwnershipSlicer
        );
        this.pageModelLocatorEvidenceSelector = new PageModelLocatorEvidenceSelector(
                locatorEvidenceClassifier == null ? new LocatorEvidenceClassifier() : locatorEvidenceClassifier,
                locatorSafetyPolicy,
                safeOwnershipSlicer
        );
    }

    public List<PromptLocatorEvidence> select(AiContextPackage context, PromptPageScope scope) {
        if (context == null || scope == null || scope.targetPage() == null) {
            return List.of();
        }
        return deduplicateLocators(locatorEvidence(context, scope.targetPage()));
    }

    private List<PromptLocatorEvidence> locatorEvidence(AiContextPackage context, MappedPage targetPage) {
        List<PromptLocatorEvidence> locators = new ArrayList<>();
        locators.addAll(enrichmentStableLocators(context, targetPage));
        locators.addAll(componentLocatorEvidenceSelector.select(context, targetPage));
        locators.addAll(dbStableLocatorEvidenceSelector.select(context, targetPage));
        locators.addAll(mappedKnowledgeLocators(targetPage));
        if (locators.isEmpty()) {
            locators.addAll(pageModelLocatorEvidenceSelector.select(context, targetPage));
        }
        return locators;
    }

    private List<PromptLocatorEvidence> enrichmentStableLocators(AiContextPackage context, MappedPage targetPage) {
        if (context == null || context.pageModelEnrichments().isEmpty() || targetPage == null) {
            return List.of();
        }
        List<PromptLocatorEvidence> locators = new ArrayList<>();
        for (PageModelEnrichmentRecord record : context.pageModelEnrichments()) {
            if (!enrichmentBelongsToTarget(record, targetPage)) {
                continue;
            }
            for (String stableLocator : record.stableLocators()) {
                parseEnrichmentLocator(stableLocator, record)
                        .ifPresent(locators::add);
            }
        }
        return locators;
    }

    private boolean enrichmentBelongsToTarget(PageModelEnrichmentRecord record, MappedPage targetPage) {
        if (record == null || targetPage == null) {
            return false;
        }
        String targetRoute = locatorSafeRoute(targetPage);
        return !record.pageId().isBlank() && record.pageId().equalsIgnoreCase(targetPage.pageId())
                || !record.pageName().isBlank() && fieldHint(record.pageName()).equalsIgnoreCase(fieldHint(targetPage.pageName()))
                || RouteCanonicalizer.routeEqualsOrSuffix(record.route(), targetRoute);
    }

    private java.util.Optional<PromptLocatorEvidence> parseEnrichmentLocator(
            String stableLocator,
            PageModelEnrichmentRecord record
    ) {
        String value = stableLocator == null ? "" : stableLocator.trim();
        int equalsIndex = value.indexOf('=');
        if (equalsIndex <= 0) {
            return java.util.Optional.empty();
        }
        String strategy = value.substring(0, equalsIndex).trim().toLowerCase(Locale.ROOT);
        int metadataIndex = value.indexOf(" (", equalsIndex + 1);
        String locatorValue = metadataIndex > equalsIndex
                ? value.substring(equalsIndex + 1, metadataIndex).trim()
                : value.substring(equalsIndex + 1).trim();
        if (strategy.isBlank() || locatorValue.isBlank()) {
            return java.util.Optional.empty();
        }
        double score = extractDouble(value, "stability=", 0.0d);
        boolean sameOrigin = !value.toLowerCase(Locale.ROOT).contains("sameorigin=false");
        if (!sameOrigin || score < 0.75d) {
            return java.util.Optional.empty();
        }
        String element = firstNonBlank(extractToken(value, "element="), semanticNameFromLocatorValue(locatorValue));
        String href = extractToken(value, "href=");
        return java.util.Optional.of(new PromptLocatorEvidence(
                fieldHint(element),
                element,
                strategy,
                locatorValue,
                inferEnrichmentRole(element, locatorValue),
                element,
                href,
                true,
                score,
                inferEnrichmentComponent(element, locatorValue),
                inferEnrichmentComponentType(element, locatorValue),
                1,
                1,
                true,
                LocatorEvidenceType.CONFIRMED_LOCATOR,
                List.of(
                        "enrichment-stable-locator:" + record.pageId(),
                        "enrichment-route:" + record.route(),
                        "evidenceType:" + LocatorEvidenceType.CONFIRMED_LOCATOR,
                        "source:page-model-enrichment"
                )
        ));
    }

    private String locatorSafeRoute(MappedPage targetPage) {
        String route = targetPage.urlPattern();
        return route == null || route.isBlank() ? targetPage.url() : route;
    }

    private double extractDouble(String value, String key, double fallback) {
        String token = extractToken(value, key);
        if (token.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String extractToken(String value, String key) {
        String source = value == null ? "" : value;
        int index = source.toLowerCase(Locale.ROOT).indexOf(key.toLowerCase(Locale.ROOT));
        if (index < 0) {
            return "";
        }
        int start = index + key.length();
        int end = source.length();
        for (int cursor = start; cursor < source.length(); cursor++) {
            char ch = source.charAt(cursor);
            if (ch == ',' || ch == ')') {
                end = cursor;
                break;
            }
        }
        return source.substring(start, end).trim();
    }

    private String inferEnrichmentRole(String element, String locatorValue) {
        String evidence = (element + " " + locatorValue).toLowerCase(Locale.ROOT);
        if (evidence.contains("user menu") || evidence.contains("userdropdown") || evidence.contains("dropdown-tab")) {
            return "button";
        }
        if (evidence.contains("href") || evidence.startsWith("a[") || evidence.contains("logout")) {
            return "link";
        }
        if (evidence.contains("input")) {
            return "input";
        }
        return "unknown";
    }

    private String inferEnrichmentComponent(String element, String locatorValue) {
        String evidence = (element + " " + locatorValue).toLowerCase(Locale.ROOT);
        if (evidence.contains("user menu") || evidence.contains("logout") || evidence.contains("dashboard")) {
            return "NavigationComponent";
        }
        return "";
    }

    private String inferEnrichmentComponentType(String element, String locatorValue) {
        return inferEnrichmentComponent(element, locatorValue).isBlank() ? "" : "NAVIGATION";
    }

    private String semanticNameFromLocatorValue(String locatorValue) {
        String normalized = locatorValue == null ? "" : locatorValue.trim();
        if (normalized.contains("oxd-userdropdown-tab") || normalized.contains("userdropdown")) {
            return "User menu trigger";
        }
        if (normalized.contains("auth/logout")) {
            return "Logout";
        }
        if (normalized.contains("dashboard/index")) {
            return "Dashboard";
        }
        if (normalized.contains("=")) {
            normalized = normalized.substring(normalized.lastIndexOf('=') + 1);
        }
        normalized = normalized.replaceAll("^[\"'\\[]+|[\"'\\]]+$", "");
        normalized = normalized.replaceAll("[^A-Za-z0-9]+", " ").trim();
        return normalized.isBlank() ? "element" : normalized;
    }

    private List<PromptLocatorEvidence> mappedKnowledgeLocators(MappedPage targetPage) {
        List<PromptLocatorEvidence> locators = new ArrayList<>();
        targetPage.forms().forEach(form -> form.fields().forEach(field ->
                locatorSelector.select(field.locatorCandidates(), field.fieldName(), field.fieldType(), field.label())
                        .ifPresent(locator -> locators.add(toLocatorEvidence(
                                field.fieldName(),
                                field.fieldType(),
                                field.label(),
                                locator
                        )))));
        for (MappedElement element : targetPage.elements()) {
            locatorSelector.select(element.locatorCandidates(), element.semanticName(), element.role(), element.text())
                    .ifPresent(locator -> locators.add(toLocatorEvidence(
                            element.semanticName(),
                            element.role(),
                            element.text(),
                            locator
                    )));
        }
        return locators;
    }

    private PromptLocatorEvidence toLocatorEvidence(
            String elementName,
            String role,
            String visibleText,
            LocatorCandidate locator
    ) {
        String normalizedName = normalizeElementName(elementName, locator, role, visibleText);
        return new PromptLocatorEvidence(
                fieldHint(normalizedName),
                normalizedName,
                locator.strategy().wireName(),
                locator.value(),
                normalizeRole(role.isBlank() ? locator.elementRole() : role),
                visibleText.isBlank() ? locator.visibleText() : visibleText,
                locator.href(),
                locator.sameOrigin(),
                locator.stabilityScore(),
                "",
                "",
                locator.uniqueOnPage() ? 1 : -1,
                locator.uniqueOnPage() ? 1 : -1,
                locator.uniqueOnPage(),
                locator.evidenceType(),
                List.of(
                        "curated-locator:" + locator.evidenceSource(),
                        "evidenceType:" + locator.evidenceType()
                )
        );
    }

    private List<PromptLocatorEvidence> deduplicateLocators(List<PromptLocatorEvidence> locators) {
        Map<String, PromptLocatorEvidence> deduped = new LinkedHashMap<>();
        for (PromptLocatorEvidence locator : locators) {
            String key = locator.strategy().toLowerCase(Locale.ROOT) + "::"
                    + locator.value().toLowerCase(Locale.ROOT);
            deduped.putIfAbsent(key, locator);
        }
        return new ArrayList<>(deduped.values());
    }

    private String normalizeElementName(
            String elementName,
            LocatorCandidate locator,
            String role,
            String visibleText
    ) {
        String candidate = firstNonBlank(elementName, visibleText, locator == null ? "" : locator.accessibleName());
        String normalized = candidate == null ? "" : candidate.trim();
        if (isGenericElementName(normalized)) {
            normalized = firstNonBlank(
                    visibleText,
                    locator == null ? "" : locator.accessibleName(),
                    semanticNameFromLocator(locator),
                    normalized
            );
        }
        if (normalized.isBlank()) {
            normalized = semanticNameFromLocator(locator);
        }
        return fieldHint(normalized);
    }

    private boolean isGenericElementName(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        return normalized.isBlank()
                || normalized.equals("field")
                || normalized.equals("input")
                || normalized.equals("password")
                || normalized.equals("passwordinput")
                || normalized.equals("button")
                || normalized.equals("link")
                || normalized.equals("element");
    }

    private String semanticNameFromLocator(LocatorCandidate locator) {
        if (locator == null) {
            return "element";
        }
        String value = locator.value();
        String normalized = value == null ? "" : value.trim();
        if (normalized.contains("=")) {
            normalized = normalized.substring(normalized.lastIndexOf('=') + 1);
        }
        normalized = normalized.replaceAll("^[\"'\\[]+|[\"'\\]]+$", "");
        normalized = normalized.replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (normalized.equalsIgnoreCase("submit")) {
            return "submitButton";
        }
        return normalized.isBlank() ? "element" : normalized;
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
        String normalized = value == null ? "" : value.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "element";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            builder.append(parts[index].substring(0, 1).toUpperCase()).append(parts[index].substring(1));
        }
        return builder.toString();
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
