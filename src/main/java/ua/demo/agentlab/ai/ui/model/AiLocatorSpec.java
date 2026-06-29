package ua.demo.agentlab.ai.ui.model;

public record AiLocatorSpec(
        String fieldName,
        String elementName,
        String strategy,
        String value
) {
    public AiLocatorSpec {
        fieldName = fieldName == null ? "" : fieldName.trim();
        elementName = elementName == null ? "" : elementName.trim();
        strategy = strategy == null ? "" : strategy.trim();
        value = value == null ? "" : value.trim();
    }
}
