package ua.demo.agentlab.api.model;

import java.util.List;

public record CanonicalApiTestCaseBundle(
        String source,
        List<CanonicalApiTestCase> testCases
) {
    public CanonicalApiTestCaseBundle {
        source = source == null ? "" : source.trim();
        testCases = testCases == null ? List.of() : List.copyOf(testCases);
    }
}
