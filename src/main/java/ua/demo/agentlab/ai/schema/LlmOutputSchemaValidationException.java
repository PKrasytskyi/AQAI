package ua.demo.agentlab.ai.schema;

public class LlmOutputSchemaValidationException extends RuntimeException {

    private final LlmOutputSchemaValidationReport report;

    public LlmOutputSchemaValidationException(LlmOutputSchemaValidationReport report) {
        super(message(report));
        this.report = report;
    }

    public LlmOutputSchemaValidationReport report() {
        return report;
    }

    private static String message(LlmOutputSchemaValidationReport report) {
        if (report == null) {
            return "LLM output schema validation failed";
        }
        return "LLM output schema validation failed for " + report.schemaVersion()
                + " with " + report.issues().size() + " issue(s)";
    }
}
