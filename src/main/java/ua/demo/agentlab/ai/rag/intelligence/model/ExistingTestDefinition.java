package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

public record ExistingTestDefinition(
        String className,
        String packageName,
        String relativePath,
        String testFramework,
        List<String> testMethods
) {
    public ExistingTestDefinition {
        testMethods = testMethods == null ? List.of() : List.copyOf(testMethods);
    }
}
