package ua.demo.agentlab.ui.discovery.component.strategy;

import ua.demo.agentlab.ui.discovery.component.model.ComponentType;

import java.util.List;

public record ComponentCandidate(
        String idSuffix,
        String name,
        ComponentType type,
        List<String> elementIds,
        double confidence,
        List<String> risks,
        List<String> sourceTrace
) {
    public ComponentCandidate {
        elementIds = elementIds == null ? List.of() : elementIds.stream().filter(id -> id != null && !id.isBlank())
                .distinct().toList();
        risks = risks == null ? List.of() : List.copyOf(risks);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }
}
