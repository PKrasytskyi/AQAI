package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

public record JavaAstMethod(
        String name,
        String returnType,
        List<String> annotations,
        List<String> parameterTypes,
        List<String> invocationTargets,
        boolean testMethod
) {
    public JavaAstMethod {
        name = name == null ? "" : name.trim();
        returnType = returnType == null ? "" : returnType.trim();
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
        parameterTypes = parameterTypes == null ? List.of() : List.copyOf(parameterTypes);
        invocationTargets = invocationTargets == null ? List.of() : List.copyOf(invocationTargets);
    }
}
