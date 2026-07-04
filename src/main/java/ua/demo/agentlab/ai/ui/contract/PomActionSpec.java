package ua.demo.agentlab.ai.ui.contract;

import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;

import java.util.List;

public record PomActionSpec(
        String methodName,
        List<AiMethodParameterSpec> parameters,
        List<PomStepSpec> steps
) {
    public PomActionSpec {
        methodName = safe(methodName);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
