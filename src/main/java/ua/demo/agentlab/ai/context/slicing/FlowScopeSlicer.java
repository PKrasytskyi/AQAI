package ua.demo.agentlab.ai.context.slicing;

import ua.demo.agentlab.ai.context.AiContextScope;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.flow.model.CanonicalFlow;
import ua.demo.agentlab.ui.flow.model.CanonicalPage;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Slices flows against an already-confirmed target scope. */
public final class FlowScopeSlicer {

    public CanonicalPageFlowModel slice(CanonicalPageFlowModel source, TargetPageScopeResolver.TargetPageScope target) {
        if (source == null) return null;
        AiContextScope scope = target.requested();
        List<CanonicalFlow> flows = source.flows().stream().filter(flow -> matches(flow, scope)).toList();
        if (flows.isEmpty()) flows = source.flows().stream().limit(6).toList();
        Set<String> names = new LinkedHashSet<>(scope.targetPageNames());
        flows.forEach(flow -> {
            add(names, flow.sourcePageName());
            add(names, flow.targetPageName());
        });
        List<CanonicalPage> pages = source.pages().stream()
                .filter(page -> names.isEmpty() || PageReferenceMatcher.matchesAny(page, names)).toList();
        return new CanonicalPageFlowModel(source.projectProfileId(), source.projectName(), pages, flows);
    }

    private boolean matches(CanonicalFlow flow, AiContextScope scope) {
        return flow.sourceRequirementIds().stream().anyMatch(scope.targetRequirementIds()::contains)
                || scope.targetPageNames().stream().anyMatch(reference ->
                PageReferenceMatcher.matchesScenarioPage(flow.sourcePageName(), flow.sourceRoute(), reference)
                        || PageReferenceMatcher.matchesScenarioPage(flow.targetPageName(), flow.targetRoute(), reference))
                || routeMatches(scope.targetRoutes(), flow.sourceRoute()) || routeMatches(scope.targetRoutes(), flow.targetRoute());
    }

    private boolean routeMatches(List<String> routes, String candidate) {
        return routes.stream().anyMatch(route -> PageReferenceMatcher.routeMatches(route, candidate));
    }

    private void add(Set<String> values, String value) {
        if (value != null && !value.isBlank()) values.add(value);
    }
}
