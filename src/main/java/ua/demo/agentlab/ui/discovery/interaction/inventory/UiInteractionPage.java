package ua.demo.agentlab.ui.discovery.interaction.inventory;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;

import java.util.List;

public record UiInteractionPage(
        String pageId,
        String pageName,
        String route,
        String capability,
        String pageFingerprintHash,
        KnowledgeRunMetadata runMetadata,
        List<SemanticComponentInventory> components,
        List<String> sourceTrace
) {
    public UiInteractionPage {
        pageId = safe(pageId);
        pageName = safe(pageName);
        route = safe(route);
        capability = safe(capability);
        pageFingerprintHash = safe(pageFingerprintHash);
        components = components == null ? List.of() : List.copyOf(components);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
