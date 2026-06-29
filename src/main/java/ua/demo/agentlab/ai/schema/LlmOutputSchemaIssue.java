package ua.demo.agentlab.ai.schema;

public record LlmOutputSchemaIssue(
        String path,
        String message
) {
    public LlmOutputSchemaIssue {
        path = path == null || path.isBlank() ? "$" : path.trim();
        message = message == null ? "" : message.trim();
    }
}
