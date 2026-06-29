package ua.demo.agentlab.ai.flow;

import ua.demo.agentlab.ai.context.CanonicalUiInteractionModel;
import ua.demo.agentlab.ai.context.UiKnowledgeRetrievalContext;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.List;

public record FlowScopedKnowledgePackage(
        BusinessFlowContext flowContext,
        List<String> selectedPageIds,
        List<String> selectedGraphNodeIds,
        MappedUiKnowledge mappedUiKnowledge,
        CanonicalUiInteractionModel canonicalInteractionModel,
        UiKnowledgeRetrievalContext retrievalContext,
        List<String> excludedSignals
) {
    public FlowScopedKnowledgePackage {
        selectedPageIds = selectedPageIds == null ? List.of() : List.copyOf(selectedPageIds);
        selectedGraphNodeIds = selectedGraphNodeIds == null ? List.of() : List.copyOf(selectedGraphNodeIds);
        mappedUiKnowledge = mappedUiKnowledge == null
                ? new MappedUiKnowledge(List.of(), List.of(), List.of(), List.of(), List.of())
                : mappedUiKnowledge;
        canonicalInteractionModel = canonicalInteractionModel == null
                ? CanonicalUiInteractionModel.empty()
                : canonicalInteractionModel;
        retrievalContext = retrievalContext == null
                ? UiKnowledgeRetrievalContext.empty("Flow-scoped retrieval context is not available")
                : retrievalContext;
        excludedSignals = excludedSignals == null ? List.of() : List.copyOf(excludedSignals);
    }
}
