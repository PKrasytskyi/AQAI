package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.AiContextScope;
import ua.demo.agentlab.ai.context.AiContextScopeResolver;
import ua.demo.agentlab.ai.context.TargetAwareContextSlicer;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AiPageObjectScopeResolverStage {

    private final AiContextScopeResolver scopeResolver;
    private final TargetAwareContextSlicer contextSlicer;

    public AiPageObjectScopeResolverStage() {
        this(new AiContextScopeResolver(), new TargetAwareContextSlicer());
    }

    AiPageObjectScopeResolverStage(
            AiContextScopeResolver scopeResolver,
            TargetAwareContextSlicer contextSlicer
    ) {
        if (scopeResolver == null || contextSlicer == null) {
            throw new IllegalArgumentException("scope collaborators cannot be null");
        }
        this.scopeResolver = scopeResolver;
        this.contextSlicer = contextSlicer;
    }

    public List<AiPageObjectPromptScope> resolve(AiPageObjectGenerationRequest request) {
        if (request == null || request.contextPackage() == null || request.uiTestPlan() == null) {
            return List.of();
        }
        return request.uiTestPlan().pageNames().stream()
                .map(pageName -> resolvePage(request.contextPackage(), pageName, request.baselineSpecs()))
                .toList();
    }

    private AiPageObjectPromptScope resolvePage(
            AiContextPackage contextPackage,
            String pageName,
            List<AiPageObjectSpec> baselineSpecs
    ) {
        AiContextScope pageScope = scopeResolver.resolveForPage(contextPackage, pageName);
        AiContextPackage scopedContext = contextSlicer.slice(contextPackage, pageScope);
        List<UiTestScenario> pageScenarios = scopedContext.uiTestPlan() == null
                ? List.of()
                : scopedContext.uiTestPlan().scenarios();
        AiPageObjectSpec baselineSpec = baselineSpecs == null ? null : baselineSpecs.stream()
                .filter(spec -> PageReferenceMatcher.matchesScenarioPage(spec.pageName(), spec.route(), pageName))
                .findFirst()
                .orElse(null);
        return new AiPageObjectPromptScope(
                pageName,
                sanitizeFileStem(pageName),
                pageScope,
                contextPackage,
                scopedContext,
                pageScenarios,
                baselineSpec,
                buildScopeTrace(pageName, pageScope, contextPackage, scopedContext, pageScenarios)
        );
    }

    private Map<String, Object> buildScopeTrace(
            String pageName,
            AiContextScope pageScope,
            AiContextPackage originalContext,
            AiContextPackage scopedContext,
            List<UiTestScenario> pageScenarios
    ) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("requestedPage", pageName);
        trace.put("targetPageNames", pageScope == null ? List.of() : pageScope.targetPageNames());
        trace.put("targetRoutes", pageScope == null ? List.of() : pageScope.targetRoutes());
        trace.put("matchedScenarioIds", pageScenarios == null
                ? List.of()
                : pageScenarios.stream().map(UiTestScenario::id).toList());
        trace.put("rejectedScenarios", rejectedScenarios(pageName, originalContext, pageScenarios));
        trace.put("matchedCanonicalCaseIds", scopedContext == null || scopedContext.canonicalTestCaseBundle() == null
                ? List.of()
                : scopedContext.canonicalTestCaseBundle().testCases().stream().map(testCase -> testCase.id()).toList());
        trace.put("matchedMappedPages", scopedContext == null || scopedContext.mappedUiKnowledge() == null
                ? List.of()
                : scopedContext.mappedUiKnowledge().pages().stream()
                .map(page -> Map.of(
                        "pageId", page.pageId(),
                        "pageName", page.pageName(),
                        "pageType", page.pageType(),
                        "urlPattern", page.urlPattern()
                ))
                .toList());
        trace.put("matchedPageModels", scopedContext == null || scopedContext.pageModelBundle() == null
                ? List.of()
                : scopedContext.pageModelBundle().pages().stream()
                .map(page -> Map.of(
                        "pageId", page.pageId(),
                        "route", page.route(),
                        "featureGuess", page.featureGuess(),
                        "elements", page.elements().size(),
                        "forms", page.forms().size()
                ))
                .toList());
        trace.put("routeCollisions", routeCollisions(originalContext));
        return trace;
    }

    private List<Map<String, Object>> rejectedScenarios(
            String pageName,
            AiContextPackage originalContext,
            List<UiTestScenario> pageScenarios
    ) {
        if (originalContext == null || originalContext.uiTestPlan() == null) {
            return List.of();
        }
        Set<String> matchedIds = pageScenarios == null
                ? Set.of()
                : pageScenarios.stream().map(UiTestScenario::id).collect(Collectors.toSet());
        return originalContext.uiTestPlan().scenarios().stream()
                .filter(scenario -> !matchedIds.contains(scenario.id()))
                .map(scenario -> {
                    Map<String, Object> rejected = new LinkedHashMap<>();
                    rejected.put("id", scenario.id());
                    rejected.put("sourcePageName", scenario.sourcePageName());
                    rejected.put("pageName", scenario.pageName());
                    rejected.put("reason", "Scenario source/page/prerequisite did not match requested page " + pageName);
                    return rejected;
                })
                .toList();
    }

    private List<Map<String, Object>> routeCollisions(AiContextPackage context) {
        if (context == null || context.mappedUiKnowledge() == null) {
            return List.of();
        }
        Map<String, List<String>> pagesByRoute = context.mappedUiKnowledge().pages().stream()
                .collect(Collectors.groupingBy(
                        page -> page.urlPattern() == null || page.urlPattern().isBlank() ? page.url() : page.urlPattern(),
                        LinkedHashMap::new,
                        Collectors.mapping(page -> page.pageName(), Collectors.toList())
                ));
        return pagesByRoute.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue().size() > 1)
                .map(entry -> {
                    Map<String, Object> collision = new LinkedHashMap<>();
                    collision.put("route", entry.getKey());
                    collision.put("pages", entry.getValue());
                    return collision;
                })
                .toList();
    }

    private String sanitizeFileStem(String value) {
        return value == null || value.isBlank()
                ? "page"
                : value.replaceAll("[^a-zA-Z0-9._-]", "-");
    }
}
