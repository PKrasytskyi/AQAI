package ua.demo.agentlab.ui.testcontract.model;

public record UiTestArgumentSpec(
        String parameterName,
        UiTestArgumentSource source,
        String referenceId,
        String field,
        String literalValue
) {
    public UiTestArgumentSpec {
        parameterName = safe(parameterName);
        source = source == null ? UiTestArgumentSource.DATA_REFERENCE : source;
        referenceId = safe(referenceId);
        field = safe(field);
        literalValue = safe(literalValue);
    }

    public static UiTestArgumentSpec dataReference(String parameterName, String referenceId, String field) {
        return new UiTestArgumentSpec(
                parameterName,
                UiTestArgumentSource.DATA_REFERENCE,
                referenceId,
                field,
                ""
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
