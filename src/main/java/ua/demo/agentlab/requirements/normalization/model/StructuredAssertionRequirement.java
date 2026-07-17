package ua.demo.agentlab.requirements.normalization.model;

/** Typed assertion declared by a capability-first requirement block. */
public record StructuredAssertionRequirement(String type, String target, String expectedValue, SourceReference sourceReference) {
    public StructuredAssertionRequirement {
        type = safe(type); target = safe(target); expectedValue = safe(expectedValue);
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
