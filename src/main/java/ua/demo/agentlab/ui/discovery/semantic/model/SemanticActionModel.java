package ua.demo.agentlab.ui.discovery.semantic.model;

import java.util.List;

public record SemanticActionModel(
        List<SemanticPageModel> pages,
        List<String> sourceTrace
) {
    public SemanticActionModel {
        pages = pages == null ? List.of() : List.copyOf(pages);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    public static SemanticActionModel empty(String reason) {
        return new SemanticActionModel(List.of(), List.of(reason == null ? "semantic-action:empty" : reason));
    }
}
