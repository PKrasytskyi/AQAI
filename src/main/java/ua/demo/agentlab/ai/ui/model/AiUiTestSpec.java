package ua.demo.agentlab.ai.ui.model;

import java.util.List;

public record AiUiTestSpec(
        String scenarioId,
        String className,
        String sourcePageClassName,
        String sourcePageVariableName,
        String pageClassName,
        String pageVariableName,
        String testMethodName,
        String testDescription,
        String actionBody,
        String assertionBody,
        List<String> additionalImports
) {
    public AiUiTestSpec {
        scenarioId = scenarioId == null ? "" : scenarioId.trim();
        className = className == null ? "" : className.trim();
        sourcePageClassName = sourcePageClassName == null ? "" : sourcePageClassName.trim();
        sourcePageVariableName = sourcePageVariableName == null ? "" : sourcePageVariableName.trim();
        pageClassName = pageClassName == null ? "" : pageClassName.trim();
        pageVariableName = pageVariableName == null ? "" : pageVariableName.trim();
        testMethodName = testMethodName == null ? "" : testMethodName.trim();
        testDescription = testDescription == null ? "" : testDescription.trim();
        actionBody = actionBody == null ? "" : actionBody.strip();
        assertionBody = assertionBody == null ? "" : assertionBody.strip();
        additionalImports = additionalImports == null ? List.of() : List.copyOf(additionalImports);
    }
}
