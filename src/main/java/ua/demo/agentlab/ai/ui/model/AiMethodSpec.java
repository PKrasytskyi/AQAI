package ua.demo.agentlab.ai.ui.model;

import java.util.List;

public record AiMethodSpec(
        String returnType,
        String methodName,
        List<AiMethodParameterSpec> parameters,
        String body,
        List<String> requiredImports
) {
    public AiMethodSpec {
        returnType = returnType == null ? "" : returnType.trim();
        methodName = methodName == null ? "" : methodName.trim();
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        body = body == null ? "" : body.strip();
        requiredImports = requiredImports == null ? List.of() : List.copyOf(requiredImports);
    }
}
