package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.context.PromptActionEvidence;
import ua.demo.agentlab.ai.context.PromptAssertionEvidence;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ai.ui.prompt.scope.ConfirmedUiCatalogPomScopeProjector;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PromptPageEligibilityEvaluator {

    private final PromptEvidenceProjectionInvariantGate projectionInvariantGate =
            new PromptEvidenceProjectionInvariantGate();
    private final ConfirmedUiCatalogPomScopeProjector catalogProjector =
            new ConfirmedUiCatalogPomScopeProjector();

    public PromptPage evaluate(AiPageObjectPromptScope scope) {
        if (scope == null || scope.scopedContext() == null) {
            return new PromptPage("", "", false, false, false, false, false,
                    List.of("missing scoped context"));
        }
        String pageName = scope.pageName();
        String route = resolveRoute(scope);
        boolean hasRawEvidence = hasRawEvidence(scope);
        PromptReadyPomScope sanitizedScope = catalogProjector.project(
                scope.confirmedUiCatalog(), scope.pageName());
        projectionInvariantGate.validate(scope, sanitizedScope);
        boolean hasAllowedLocators = !sanitizedScope.allowedLocators().isEmpty();
        boolean hasStableCacheEvidence = hasStableCacheEvidence(scope);
        boolean routeOnly = isRouteBackedContract(sanitizedScope);
        List<String> reasons = new ArrayList<>();
        if (scope.confirmedUiCatalog() == null || sanitizedScope.targetRoute().isBlank()) {
            reasons.add("confirmed catalog has no page-owned evidence");
        }
        if (hasRawEvidence) {
            reasons.add("raw DOM evidence is available");
        }
        if (hasAllowedLocators) {
            reasons.add("mapper-approved allowed locators are available");
        }
        if (hasStableCacheEvidence) {
            reasons.add("stable cache evidence is available");
        }
        reasons.add("sanitized scope: actions=" + sanitizedScope.ownedActions().size()
                + ", assertions=" + sanitizedScope.ownedAssertions().size()
                + ", locators=" + sanitizedScope.allowedLocators().size()
                + ", confirmedCatalogLocators=" + scope.scopedContext().confirmedCatalogLocatorEvidence().size());
        if (routeOnly) {
            reasons.add("route-only scope uses inherited BasePage navigation and does not require a generated POM");
        }
        boolean eligible = !routeOnly && (hasRawEvidence || hasAllowedLocators || hasStableCacheEvidence);
        if (!eligible) {
            if (!routeOnly) {
                reasons.add("page has no raw DOM evidence, no allowed locators, or stable cache evidence");
            }
        }
        return new PromptPage(
                pageName,
                route,
                eligible,
                routeOnly,
                hasRawEvidence,
                hasAllowedLocators,
                hasStableCacheEvidence,
                reasons
        );
    }

    private boolean hasRawEvidence(AiPageObjectPromptScope scope) {
        if (scope.scopedContext().pageModelBundle() != null) {
            for (PageModel page : scope.scopedContext().pageModelBundle().pages()) {
                if (page.evidence() != null
                        && (!page.evidence().htmlPath().isBlank() || !page.evidence().screenshotPath().isBlank())) {
                    return true;
                }
            }
        }
        if (scope.scopedContext().mappedUiKnowledge() != null) {
            for (MappedPage page : scope.scopedContext().mappedUiKnowledge().pages()) {
                if (!page.htmlPath().isBlank() || !page.screenshotPath().isBlank()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasStableCacheEvidence(AiPageObjectPromptScope scope) {
        if (scope.scopeTrace() == null) {
            return false;
        }
        Object matchedPages = scope.scopeTrace().get("matchedMappedPages");
        return matchedPages != null
                && matchedPages.toString().toLowerCase(Locale.ROOT).contains("db_stable_cache");
    }

    private boolean isRouteBackedContract(PromptReadyPomScope scope) {
        if (scope == null || !scope.allowedLocators().isEmpty()) {
            return false;
        }
        boolean actionsAreRouteOnly = scope.ownedActions().isEmpty()
                || scope.ownedActions().stream().allMatch(this::routeOnlyAction);
        boolean hasRouteAssertion = scope.ownedAssertions().stream()
                .anyMatch(assertion -> routeOnlyAssertion(new PromptAssertionEvidence(
                        assertion.type(), assertion.expectedValue(), assertion.ownerPage(), assertion.sourceTrace(), assertion.confidence()
                )));
        return actionsAreRouteOnly && hasRouteAssertion;
    }

    private boolean routeOnlyAction(String action) {
        String text = normalize(action);
        return text.isBlank()
                || text.contains("open")
                || text.contains("navigate")
                || text.contains("verify page access")
                || text.contains("inspect page content")
                || text.contains("open page");
    }

    private boolean routeOnlyAssertion(PromptAssertionEvidence assertion) {
        String text = normalize(assertion == null ? "" : assertion.type() + " " + assertion.expectedValue());
        return text.contains("url_contains")
                || text.contains("route_equals")
                || text.contains("current url")
                || text.contains("route");
    }

    private String resolveRoute(AiPageObjectPromptScope scope) {
        if (scope.scopedContext().promptUiEvidence() != null
                && !scope.scopedContext().promptUiEvidence().targetRoute().isBlank()) {
            return scope.scopedContext().promptUiEvidence().targetRoute();
        }
        if (scope.confirmedUiCatalog() != null) {
            return scope.confirmedUiCatalog().pages().stream()
                    .filter(page -> ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher.matchesScenarioPage(
                            page.pageName(), page.route(), scope.pageName()))
                    .map(ua.demo.agentlab.ui.discovery.catalog.ConfirmedCatalogPage::route)
                    .filter(route -> !route.isBlank())
                    .findFirst()
                    .orElse("");
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
