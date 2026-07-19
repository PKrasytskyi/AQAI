package ua.demo.agentlab.ui.discovery.component.strategy;

import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;

import java.util.List;

public final class NavigationComponentDetector implements ComponentDetectionStrategy {
    @Override public List<ComponentCandidate> detect(ComponentDetectionContext context) {
        List<String> ids = context.elementsById().values().stream().filter(PageElementModel::visible)
                .filter(element -> "a".equalsIgnoreCase(element.tag()) || element.technicalType().contains("LINK")
                        || "navigation".equalsIgnoreCase(element.role()))
                .map(PageElementModel::elementId).toList();
        return ids.size() < 2 ? List.of() : List.of(new ComponentCandidate("navigation", "NavigationComponent",
                ComponentType.NAVIGATION, ids, 0.78d,
                List.of("navigation-actions-are-not-page-owned-without-requirement-scope"),
                List.of("component:navigation-link-group")));
    }
}
