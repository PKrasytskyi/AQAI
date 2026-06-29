package ua.demo.agentlab.mcp.model;

import java.util.List;

public record CanonicalTestCase(
        String id,
        String title,
        String preconditions,
        List<String> steps,
        String expectedResult,
        String priority,
        List<String> labels
) {
}
