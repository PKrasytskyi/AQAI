package ua.demo.agentlab.ui.testcontract.model;

public record UiTestDataReferenceSpec(
        String id,
        UiTestDataReferenceType type,
        String key
) {
    public UiTestDataReferenceSpec {
        id = safe(id);
        type = type == null ? UiTestDataReferenceType.SCENARIO_DATA : type;
        key = safe(key);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
