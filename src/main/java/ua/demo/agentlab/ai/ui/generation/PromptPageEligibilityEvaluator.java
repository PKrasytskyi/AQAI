package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.context.PromptActionEvidence;
import ua.demo.agentlab.ai.context.PromptAssertionEvidence;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PromptPageEligibilityEvaluator {

    public PromptPage evaluate(AiPageObjectPromptScope scope) {
        if (scope == null || scope.scopedContext() == null) {
            return new PromptPage("", "", false, false, false, false, false,
                    List.of("missing scoped context"));
        }
        String pageName = scope.pageName();
        String route = resolveRoute(scope);
        boolean hasRawEvidence = hasRawEvidence(scope);
        boolean hasAllowedLocators = scope.scopedContext().promptUiEvidence() != null
                && !scope.scopedContext().promptUiEvidence().requiredLocators().isEmpty();
        boolean hasStableCacheEvidence = hasStableCacheEvidence(scope);
        boolean routeOnly = isRouteBackedContract(scope);
        List<String> reasons = new ArrayList<>();
        if (hasRawEvidence) {
            reasons.add("raw DOM evidence is available");
        }
        if (hasAllowedLocators) {
            reasons.add("mapper-approved allowed locators are available");
        }
        if (hasStableCacheEvidence) {
            reasons.add("stable cache evidence is available");
        }
        if (routeOnly) {
                reasons.add("route-backed page contract; locator-backed checks require coverage gaps when evidence is missing");
        }
        boolean eligible = hasRawEvidence || hasAllowedLocators || hasStableCacheEvidence || routeOnly;
        if (!eligible) {
            reasons.add("page has no raw DOM evidence, no allowed locators, no stable cache evidence, and is not route-only");
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

    private boolean isRouteBackedContract(AiPageObjectPromptScope scope) {
        if (scope.scopedContext().promptUiEvidence() == null) {
            return false;
        }
        boolean hasLocators = !scope.scopedContext().promptUiEvidence().requiredLocators().isEmpty();
        if (hasLocators) {
            return false;
        }
        boolean actionsAreRouteOnly = scope.scopedContext().promptUiEvidence().requiredActions().isEmpty()
                || scope.scopedContext().promptUiEvidence().requiredActions().stream()
                .allMatch(this::routeOnlyAction);
        boolean hasRouteAssertion = scope.scopedContext().promptUiEvidence().requiredAssertions().stream()
                .anyMatch(this::routeOnlyAssertion);
        return actionsAreRouteOnly && hasRouteAssertion;
    }

    private boolean routeOnlyAction(PromptActionEvidence action) {
        String text = normalize(action == null ? "" : action.name() + " " + action.type());
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
        if (scope.scopedContext().mappedUiKnowledge() != null
                && !scope.scopedContext().mappedUiKnowledge().pages().isEmpty()) {
            MappedPage page = scope.scopedContext().mappedUiKnowledge().pages().get(0);
            return page.urlPattern().isBlank() ? page.url() : page.urlPattern();
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
