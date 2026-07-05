package ua.demo.agentlab.ui.discovery.evidence;

import ua.demo.agentlab.ui.discovery.component.model.ComponentDiscoveryModel;
import ua.demo.agentlab.ui.discovery.component.model.ScopedLocatorCandidate;
import ua.demo.agentlab.ui.discovery.component.model.SemanticComponentModel;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageApiRelationModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.semantic.model.BusinessIntentCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticElementModel;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticPageModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EvidenceRankingService {

    private final LocatorEvidenceClassifier evidenceClassifier = new LocatorEvidenceClassifier();

    public List<POMRelevantEvidence> rank(
            MappedPage targetPage,
            PageModel pageModel,
            ComponentDiscoveryModel componentModel,
            SemanticPageModel semanticPage,
            Set<String> requirementIds
    ) {
        if (targetPage == null || pageModel == null || componentModel == null) {
            return List.of();
        }
        Map<String, PageElementModel> elementsById = pageModel.elements().stream()
                .collect(java.util.stream.Collectors.toMap(
                        PageElementModel::elementId,
                        element -> element,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, SemanticElementModel> semanticElementsById = semanticElementsById(semanticPage);
        List<POMRelevantEvidence> ranked = new ArrayList<>();
        for (var componentPage : componentModel.pages()) {
            if (!componentPage.pageId().equalsIgnoreCase(pageModel.pageId())
                    && !RouteCanonicalizer.routeEqualsOrSuffix(componentPage.route(), route(targetPage))) {
                continue;
            }
            for (SemanticComponentModel component : componentPage.components()) {
                for (ScopedLocatorCandidate locator : component.locators()) {
                    PageElementModel element = elementsById.get(locator.elementId());
                    if (element == null || !element.visible()) {
                        continue;
                    }
                    ranked.add(toEvidence(
                            targetPage,
                            pageModel,
                            component,
                            locator,
                            element,
                            semanticElementsById.get(element.elementId()),
                            requirementIds
                    ));
                }
            }
        }
        return ranked.stream()
                .filter(evidence -> !evidence.value().isBlank())
                .sorted(Comparator.comparingDouble(POMRelevantEvidence::finalScore).reversed())
                .toList();
    }

    private POMRelevantEvidence toEvidence(
            MappedPage targetPage,
            PageModel pageModel,
            SemanticComponentModel component,
            ScopedLocatorCandidate locator,
            PageElementModel element,
            SemanticElementModel semanticElement,
            Set<String> requirementIds
    ) {
        double locatorScore = locator.finalScore();
        double semanticConfidence = semanticConfidence(semanticElement, component);
        double runtimeEvidenceConfidence = runtimeEvidenceConfidence(pageModel, element);
        double requirementMatch = requirementIds == null || requirementIds.isEmpty() ? 0.45d : 0.72d;
        double routeMatch = routeMatch(pageModel, targetPage);
        double ownershipScore = ownershipScore(pageModel, targetPage, component);
        double riskPenalty = riskPenalty(locator.risks());
        double finalScore = locatorScore * 0.35d
                + semanticConfidence * 0.20d
                + runtimeEvidenceConfidence * 0.15d
                + requirementMatch * 0.10d
                + routeMatch * 0.10d
                + ownershipScore * 0.10d
                - riskPenalty;
        List<String> reasons = new ArrayList<>();
        reasons.add("locatorScore=" + round(locatorScore));
        reasons.add("semanticConfidence=" + round(semanticConfidence));
        reasons.add("runtimeEvidenceConfidence=" + round(runtimeEvidenceConfidence));
        reasons.add("requirementMatch=" + round(requirementMatch));
        reasons.add("routeMatch=" + round(routeMatch));
        reasons.add("ownershipScore=" + round(ownershipScore));
        if (riskPenalty > 0.0d) {
            reasons.add("riskPenalty=" + round(riskPenalty));
        }
        POMRelevantEvidence evidence = new POMRelevantEvidence(
                pageModel.pageId(),
                targetPage.pageName(),
                route(targetPage),
                element.elementId(),
                component.name(),
                component.type().name(),
                fieldHint(elementName(element, locator.value())),
                elementName(element, locator.value()),
                locator.strategy(),
                locator.value(),
                role(element),
                element.text(),
                element.href(),
                sameOrigin(element, pageModel),
                locator.uniqueWithinComponent(),
                locator.globalMatchCount(),
                locator.scopedMatchCount(),
                locatorScore,
                semanticConfidence,
                runtimeEvidenceConfidence,
                requirementMatch,
                routeMatch,
                ownershipScore,
                riskPenalty,
                finalScore,
                locator.risks(),
                reasons
        );
        return new POMRelevantEvidence(
                evidence.pageId(),
                evidence.pageName(),
                evidence.route(),
                evidence.elementId(),
                evidence.componentName(),
                evidence.componentType(),
                evidence.fieldHint(),
                evidence.elementName(),
                evidence.strategy(),
                evidence.value(),
                evidence.role(),
                evidence.visibleText(),
                evidence.href(),
                evidence.sameOrigin(),
                evidence.uniqueWithinComponent(),
                evidence.globalMatchCount(),
                evidence.scopedMatchCount(),
                evidence.locatorScore(),
                evidence.semanticConfidence(),
                evidence.runtimeEvidenceConfidence(),
                evidence.requirementMatch(),
                evidence.routeMatch(),
                evidence.ownershipScore(),
                evidence.riskPenalty(),
                evidence.finalScore(),
                evidenceClassifier.classify(evidence),
                evidence.risks(),
                evidence.reasons()
        );
    }

    private Map<String, SemanticElementModel> semanticElementsById(SemanticPageModel semanticPage) {
        if (semanticPage == null) {
            return Map.of();
        }
        return semanticPage.elements().stream()
                .collect(java.util.stream.Collectors.toMap(
                        SemanticElementModel::elementId,
                        element -> element,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private double semanticConfidence(SemanticElementModel semanticElement, SemanticComponentModel component) {
        double elementConfidence = semanticElement == null ? 0.0d : semanticElement.confidence();
        double intentConfidence = semanticElement == null
                ? 0.0d
                : semanticElement.businessIntentCandidates().stream()
                .filter(intent -> !genericIntent(intent))
                .mapToDouble(BusinessIntentCandidate::confidence)
                .max()
                .orElse(0.0d);
        return Math.max(component.confidence(), Math.max(elementConfidence, intentConfidence));
    }

    private boolean genericIntent(BusinessIntentCandidate intent) {
        String value = intent == null ? "" : intent.intent();
        return value.equals("INTERACT_WITH_ELEMENT") || value.equals("PAGE_CONTENT");
    }

    private double runtimeEvidenceConfidence(PageModel pageModel, PageElementModel element) {
        double relationScore = pageModel.apiRelations().stream()
                .filter(relation -> relation.elementId().isBlank() || relation.elementId().equals(element.elementId()))
                .mapToDouble(PageApiRelationModel::confidenceScore)
                .max()
                .orElse(0.0d);
        if (relationScore > 0.0d) {
            return relationScore;
        }
        String evidence = normalize(pageModel.visibleText() + " " + pageModel.featureGuess() + " " + element.text() + " " + element.href());
        if (containsAny(evidence, "dashboard", "api", "auth", "login", "logout", "employee", "pim")) {
            return 0.64d;
        }
        return 0.45d;
    }

    private double routeMatch(PageModel pageModel, MappedPage targetPage) {
        return RouteCanonicalizer.routeEqualsOrSuffix(pageModel.route(), route(targetPage))
                || RouteCanonicalizer.routeEqualsOrSuffix(pageModel.url(), targetPage.url())
                ? 1.0d
                : 0.35d;
    }

    private double ownershipScore(PageModel pageModel, MappedPage targetPage, SemanticComponentModel component) {
        if (pageModel.pageId().equalsIgnoreCase(targetPage.pageId())
                || component.pageId().equalsIgnoreCase(targetPage.pageId())) {
            return 1.0d;
        }
        return RouteCanonicalizer.routeEqualsOrSuffix(pageModel.route(), route(targetPage)) ? 0.85d : 0.35d;
    }

    private double riskPenalty(List<String> risks) {
        if (risks == null || risks.isEmpty()) {
            return 0.0d;
        }
        double penalty = 0.0d;
        for (String risk : risks) {
            String normalized = normalize(risk);
            if (containsAny(normalized, "external", "security-token", "hidden", "absolute", "dynamic-css", "framework-generated")) {
                penalty += 0.40d;
            } else if (containsAny(normalized, "not-component-unique", "not-globally-unique", "unstable")) {
                penalty += 0.08d;
            } else {
                penalty += 0.03d;
            }
        }
        return Math.min(0.80d, penalty);
    }

    private boolean sameOrigin(PageElementModel element, PageModel pageModel) {
        String href = element.href().toLowerCase(Locale.ROOT);
        if (href.isBlank() || href.startsWith("/") || href.startsWith("./") || href.startsWith("../")) {
            return true;
        }
        try {
            java.net.URI candidate = java.net.URI.create(href);
            java.net.URI page = java.net.URI.create(pageModel.url());
            return candidate.getScheme() != null
                    && page.getScheme() != null
                    && candidate.getScheme().equalsIgnoreCase(page.getScheme())
                    && firstNonBlank(candidate.getHost()).equalsIgnoreCase(firstNonBlank(page.getHost()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private String elementName(PageElementModel element, String locatorValue) {
        return firstNonBlank(
                element.name(),
                element.id(),
                element.ariaLabel(),
                element.placeholder(),
                element.text(),
                locatorValue.replaceAll("[^A-Za-z0-9]+", " ").trim(),
                "element"
        );
    }

    private String role(PageElementModel element) {
        String evidence = normalize(element.role() + " " + element.semanticType() + " " + element.technicalType());
        if (evidence.contains("password")) {
            return "password";
        }
        if (containsAny(evidence, "input", "field", "text")) {
            return "input";
        }
        if (containsAny(evidence, "button", "submit")) {
            return "button";
        }
        if (evidence.contains("link")) {
            return "link";
        }
        return firstNonBlank(element.role(), element.semanticType(), element.technicalType(), "unknown").toLowerCase(Locale.ROOT);
    }

    private String route(MappedPage page) {
        return page.urlPattern().isBlank() ? page.url() : page.urlPattern();
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
            builder.append(parts[index].substring(0, 1).toUpperCase(Locale.ROOT)).append(parts[index].substring(1));
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

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String round(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
