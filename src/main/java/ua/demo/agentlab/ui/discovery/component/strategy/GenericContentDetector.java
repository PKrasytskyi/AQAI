package ua.demo.agentlab.ui.discovery.component.strategy;

import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;

import java.util.List;

public final class GenericContentDetector implements ComponentDetectionStrategy {
    @Override public List<ComponentCandidate> detect(ComponentDetectionContext context) {
        List<String> ids = context.elementsById().values().stream().filter(PageElementModel::visible)
                .map(PageElementModel::elementId).toList();
        return ids.isEmpty() ? List.of() : List.of(new ComponentCandidate("content", "ContentComponent",
                ComponentType.CONTENT, ids, 0.62d, List.of("fallback-component-boundary"),
                List.of("component:unassigned-visible-elements")));
    }
}
