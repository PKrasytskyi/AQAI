package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.component.ComponentBoundaryDetector;
import ua.demo.agentlab.ui.discovery.component.model.ComponentDiscoveryModel;
import ua.demo.agentlab.ui.discovery.component.model.ScopedLocatorCandidate;
import ua.demo.agentlab.ui.discovery.component.model.SemanticComponentModel;
import ua.demo.agentlab.ui.discovery.evidence.EvidenceRankingService;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.evidence.POMRelevantEvidence;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.semantic.SemanticActionModelBuilder;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticPageModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ComponentLocatorEvidenceSelector {

    private final ComponentBoundaryDetector componentBoundaryDetector;
    private final SemanticActionModelBuilder semanticActionModelBuilder;
    private final EvidenceRankingService evidenceRankingService;
    private final PageOwnershipSlicer ownershipSlicer;

    public ComponentLocatorEvidenceSelector(
            ComponentBoundaryDetector componentBoundaryDetector,
            SemanticActionModelBuilder semanticActionModelBuilder,
            EvidenceRankingService evidenceRankingService,
            PageOwnershipSlicer ownershipSlicer
    ) {
        this.componentBoundaryDetector = componentBoundaryDetector == null
                ? new ComponentBoundaryDetector()
                : componentBoundaryDetector;
        this.semanticActionModelBuilder = semanticActionModelBuilder == null
                ? new SemanticActionModelBuilder()
                : semanticActionModelBuilder;
        this.evidenceRankingService = evidenceRankingService == null ? new EvidenceRankingService() : evidenceRankingService;
        this.ownershipSlicer = ownershipSlicer == null ? new PageOwnershipSlicer() : ownershipSlicer;
    }

    public List<PromptLocatorEvidence> select(AiContextPackage context, MappedPage targetPage) {
        if (context.pageModelBundle() == null || context.pageModelBundle().pages().isEmpty()) {
            return List.of();
        }
        ComponentDiscoveryModel componentModel = componentBoundaryDetector.detect(context.pageModelBundle());
        PageModel pageModel = context.pageModelBundle().pages().stream()
                .filter(page -> pageModelMatchesTarget(page, targetPage))
                .findFirst()
                .orElse(null);
        if (pageModel == null) {
            return List.of();
        }
        SemanticPageModel semanticPage = semanticActionModelBuilder.buildForTarget(
                context.pageModelBundle(),
                context.mappedUiKnowledge(),
                targetPage
        );
        List<PromptLocatorEvidence> locators = evidenceRankingService.rank(
                        targetPage,
                        pageModel,
                        componentModel,
                        semanticPage,
                        requirementIds(context)
                ).stream()
                .map(this::toRankedLocatorEvidence)
                // Keep enough ranked component evidence for downstream requirement-aware prioritization.
                // Protected SPA pages can have many high-scoring navigation links before the actual
                // user menu trigger, so trimming too early can remove the locator that the POM needs.
                .limit(128)
                .toList();
        if (hasLogoutMenuRequirement(context, targetPage)) {
            locators = ensureUserMenuTriggerLocator(locators, componentModel, pageModel, targetPage);
        }
        return locators;
    }

    private List<PromptLocatorEvidence> ensureUserMenuTriggerLocator(
            List<PromptLocatorEvidence> locators,
            ComponentDiscoveryModel componentModel,
            PageModel pageModel,
            MappedPage targetPage
    ) {
        if (locators.stream().anyMatch(this::isActualUserMenuTriggerLocator)) {
            return locators;
        }
        Map<String, PageElementModel> elementsById = pageModel.elements().stream()
                .collect(java.util.stream.Collectors.toMap(
                        PageElementModel::elementId,
                        element -> element,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return bestUserMenuTrigger(componentModel, pageModel, targetPage, elementsById)
                .map(locator -> {
                    List<PromptLocatorEvidence> merged = new ArrayList<>();
                    merged.add(locator);
                    merged.addAll(locators);
                    return List.copyOf(merged);
                })
                .orElse(locators);
    }

    private java.util.Optional<PromptLocatorEvidence> bestUserMenuTrigger(
            ComponentDiscoveryModel componentModel,
            PageModel pageModel,
            MappedPage targetPage,
            Map<String, PageElementModel> elementsById
    ) {
        PromptLocatorEvidence best = null;
        for (var componentPage : componentModel.pages()) {
            if (!componentPage.pageId().equalsIgnoreCase(pageModel.pageId())
                    && !RouteCanonicalizer.routeEqualsOrSuffix(componentPage.route(), ownershipSlicer.route(targetPage))) {
                continue;
            }
            for (SemanticComponentModel component : componentPage.components()) {
                for (ScopedLocatorCandidate candidate : component.locators()) {
                    if (!isActualUserMenuTriggerCandidate(candidate)) {
                        continue;
                    }
                    PageElementModel element = elementsById.get(candidate.elementId());
                    if (element == null || !element.visible()) {
                        continue;
                    }
                    PromptLocatorEvidence locator = toDependencyLocatorEvidence(component, candidate, element);
                    if (best == null || locator.stabilityScore() > best.stabilityScore()) {
                        best = locator;
                    }
                }
            }
        }
        return java.util.Optional.ofNullable(best);
    }

    private PromptLocatorEvidence toDependencyLocatorEvidence(
            SemanticComponentModel component,
            ScopedLocatorCandidate candidate,
            PageElementModel element
    ) {
        List<String> sourceTrace = new ArrayList<>();
        sourceTrace.add("dependency-locator:" + candidate.elementId());
        sourceTrace.add("component:" + component.name());
        sourceTrace.add("evidenceType:" + candidate.evidenceType());
        sourceTrace.add("reason:required-for-logout-menu-flow");
        return new PromptLocatorEvidence(
                "userMenuTrigger",
                firstNonBlank(element.name(), element.text(), "user menu trigger"),
                normalizeStrategy(candidate.strategy()),
                candidate.value(),
                "button",
                element.text(),
                element.href(),
                true,
                dependencyStabilityScore(candidate),
                component.name(),
                component.type().name(),
                candidate.globalMatchCount(),
                candidate.scopedMatchCount(),
                candidate.uniqueWithinComponent(),
                dependencyEvidenceType(candidate),
                sourceTrace
        );
    }

    private LocatorEvidenceType dependencyEvidenceType(ScopedLocatorCandidate candidate) {
        if (candidate == null) {
            return LocatorEvidenceType.CANDIDATE_LOCATOR;
        }
        return candidate == null ? LocatorEvidenceType.CANDIDATE_LOCATOR : candidate.evidenceType();
    }

    private double dependencyStabilityScore(ScopedLocatorCandidate candidate) {
        if (candidate == null) {
            return 0.0d;
        }
        return candidate.finalScore();
    }

    private boolean isActualUserMenuTriggerLocator(PromptLocatorEvidence locator) {
        if (locator == null) {
            return false;
        }
        String value = normalize(locator.value());
        if (containsAny(value,
                "a[href",
                "href=",
                "oxd-userdropdown-link",
                "logout",
                "support",
                "about",
                "change password")) {
            return false;
        }
        String evidence = normalize(String.join(" ",
                locator.fieldHint(),
                locator.elementName(),
                locator.role(),
                locator.visibleText(),
                locator.value(),
                locator.href()
        ));
        return containsAny(evidence, "userdropdown", "user menu trigger", "user-menu-trigger", "dropdown tab", "oxd-userdropdown-tab")
                && !containsAny(evidence, "support", "about", "logout", "change password");
    }

    private boolean isActualUserMenuTriggerCandidate(ScopedLocatorCandidate candidate) {
        if (candidate == null) {
            return false;
        }
        String value = normalize(candidate.value());
        String elementId = normalize(candidate.elementId());
        String evidence = elementId + " " + value;
        if (containsAny(value,
                "a[href",
                "href=",
                "oxd-userdropdown-link",
                "logout",
                "support",
                "about",
                "change password")) {
            return false;
        }
        return containsAny(value,
                "oxd-userdropdown-tab",
                "userdropdown-tab",
                "user-menu-trigger")
                || containsAny(evidence, "dropdown tab");
    }

    private PromptLocatorEvidence toRankedLocatorEvidence(POMRelevantEvidence evidence) {
        List<String> sourceTrace = new ArrayList<>();
        sourceTrace.add("ranked-evidence:" + evidence.elementId());
        sourceTrace.add("component:" + evidence.componentName());
        sourceTrace.add("evidenceType:" + evidence.evidenceType());
        sourceTrace.add("score:" + String.format(Locale.ROOT, "%.2f", evidence.finalScore()));
        sourceTrace.addAll(evidence.reasons());
        return new PromptLocatorEvidence(
                fieldHint(firstNonBlank(evidence.fieldHint(), evidence.elementName(), evidence.elementId())),
                fieldHint(firstNonBlank(evidence.elementName(), evidence.fieldHint(), evidence.elementId())),
                normalizeStrategy(evidence.strategy()),
                evidence.value(),
                normalizeRole(evidence.role()),
                evidence.visibleText(),
                evidence.href(),
                evidence.sameOrigin(),
                evidence.finalScore(),
                evidence.componentName(),
                evidence.componentType(),
                evidence.globalMatchCount(),
                evidence.scopedMatchCount(),
                evidence.uniqueWithinComponent(),
                evidence.evidenceType(),
                sourceTrace
        );
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

    private java.util.Set<String> requirementIds(AiContextPackage context) {
        java.util.Set<String> ids = new java.util.LinkedHashSet<>();
        if (context.canonicalTestCaseBundle() != null) {
            context.canonicalTestCaseBundle().testCases().forEach(testCase -> ids.addAll(testCase.requirementRefs()));
        }
        context.assertionContracts().forEach(contract -> {
            if (contract.requirementId() != null && !contract.requirementId().isBlank()) {
                ids.add(contract.requirementId().trim());
            }
        });
        return ids;
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

    private boolean hasLogoutMenuRequirement(AiContextPackage context, MappedPage targetPage) {
        if (context == null || context.canonicalTestCaseBundle() == null || targetPage == null) {
            return false;
        }
        return context.canonicalTestCaseBundle().testCases().stream()
                .filter(testCase -> ownershipSlicer.canonicalTestCaseBelongsToTarget(testCase, targetPage))
                .map(testCase -> String.join(" ",
                        safe(testCase.title()),
                        String.join(" ", testCase.actions()),
                        String.join(" ", testCase.assertions()),
                        testCase.operationIntents().stream()
                                .map(intent -> intent.kind() == null ? "" : intent.kind().name())
                                .toList()
                                .toString()))
                .map(this::normalize)
                .anyMatch(text -> containsAny(text, "logout", "sign out")
                        && containsAny(text, "user menu", "menu", "drop-down", "dropdown"));
    }

    private String locatorEvidenceText(PromptLocatorEvidence locator) {
        return String.join(" ",
                        locator.fieldHint(),
                        locator.elementName(),
                        locator.role(),
                        locator.visibleText(),
                        locator.href(),
                        locator.value(),
                        String.join(" ", locator.sourceTrace()))
                .toLowerCase(Locale.ROOT);
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

    private boolean containsAny(String value, String... fragments) {
        String normalized = normalize(value);
        for (String fragment : fragments) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
