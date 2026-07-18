package ua.demo.agentlab.ui.discovery.component.strategy;

import ua.demo.agentlab.ui.discovery.component.model.ComponentType;

import java.util.List;

public final class SearchComponentDetector implements ComponentDetectionStrategy {
    private final ComponentEvidenceMatcher matcher = new ComponentEvidenceMatcher();
    @Override public List<ComponentCandidate> detect(ComponentDetectionContext context) {
        List<String> ids = matcher.ids(context, "search", "type=search", "placeholder=search");
        return ids.isEmpty() ? List.of() : List.of(new ComponentCandidate("search", "SearchComponent",
                ComponentType.SEARCH, ids, 0.82d, List.of(), List.of("component:search-evidence")));
    }
}
