package ua.demo.agentlab.mcp.model;

import java.util.List;

public record CanonicalTestCaseBundle(
        SourceDescriptor source,
        List<CanonicalTestCase> testCases,
        List<String> warnings
) {
}
