package ua.demo.agentlab.ui.discovery.component.model;

import java.util.List;

public record ComponentDiscoveryModel(
        List<SemanticComponentPageModel> pages,
        List<String> sourceTrace
) {
    public ComponentDiscoveryModel {
        pages = pages == null ? List.of() : List.copyOf(pages);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace.stream()
                .filter(source -> source != null && !source.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }

    public static ComponentDiscoveryModel empty(String reason) {
        return new ComponentDiscoveryModel(List.of(), List.of(reason == null ? "component:empty" : reason));
    }
}
