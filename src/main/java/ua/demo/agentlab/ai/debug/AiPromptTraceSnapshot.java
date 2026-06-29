package ua.demo.agentlab.ai.debug;

import java.util.LinkedHashMap;
import java.util.Map;

public record AiPromptTraceSnapshot(
        String stage,
        String scopeId,
        String subject,
        String promptType,
        String promptFile,
        Map<String, Object> metadata
) {
    public AiPromptTraceSnapshot {
        stage = defaultValue(stage, "default");
        scopeId = defaultValue(scopeId, "global");
        subject = defaultValue(subject, "unknown");
        promptType = defaultValue(promptType, "generic");
        promptFile = defaultValue(promptFile, "");
        metadata = metadata == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(metadata));
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
