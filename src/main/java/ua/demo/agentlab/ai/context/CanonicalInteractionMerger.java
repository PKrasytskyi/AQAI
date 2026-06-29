package ua.demo.agentlab.ai.context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CanonicalInteractionMerger {

    public List<CanonicalUiInteraction> deduplicate(List<CanonicalUiInteraction> interactions) {
        Map<String, CanonicalUiInteraction> deduplicated = new LinkedHashMap<>();
        for (CanonicalUiInteraction interaction : interactions) {
            String key = interaction.pageId()
                    + "|" + interaction.interactionType()
                    + "|" + interaction.subjectType()
                    + "|" + interaction.sourceElementId();
            CanonicalUiInteraction existing = deduplicated.get(key);
            if (existing == null || interaction.confidenceScore() > existing.confidenceScore()) {
                deduplicated.put(key, interaction);
            }
        }
        return List.copyOf(deduplicated.values());
    }
}
