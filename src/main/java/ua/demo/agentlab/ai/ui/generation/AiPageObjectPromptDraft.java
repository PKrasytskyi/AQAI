package ua.demo.agentlab.ai.ui.generation;

import java.util.Map;

public record AiPageObjectPromptDraft(
        AiPageObjectPromptScope scope,
        String prompt,
        Map<String, Object> metadata
) {
    public AiPageObjectPromptDraft {
        prompt = prompt == null ? "" : prompt;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
