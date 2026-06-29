package ua.demo.agentlab.ai.ui.model;

public record AiMethodParameterSpec(
        String type,
        String name
) {
    public AiMethodParameterSpec {
        type = type == null ? "" : type.trim();
        name = name == null ? "" : name.trim();
    }
}
