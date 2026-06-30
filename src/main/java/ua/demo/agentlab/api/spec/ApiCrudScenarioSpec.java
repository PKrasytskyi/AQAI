package ua.demo.agentlab.api.spec;

public record ApiCrudScenarioSpec(
        String packageName,
        String className,
        String testMethodName,
        String clientClassName,
        String createMethodName,
        String readMethodName,
        String updateMethodName,
        String patchMethodName,
        String deleteMethodName,
        String createRequestDtoClassName,
        String updateRequestDtoClassName,
        String patchRequestDtoClassName,
        String responseDtoClassName,
        String pathParameterName,
        String idJsonPath
) {
    public ApiCrudScenarioSpec {
        packageName = packageName == null || packageName.isBlank()
                ? "ua.demo.agentlab.api.generated.tests"
                : packageName.trim();
        className = safe(className);
        testMethodName = safe(testMethodName);
        clientClassName = safe(clientClassName);
        createMethodName = safe(createMethodName);
        readMethodName = safe(readMethodName);
        updateMethodName = safe(updateMethodName);
        patchMethodName = safe(patchMethodName);
        deleteMethodName = safe(deleteMethodName);
        createRequestDtoClassName = safe(createRequestDtoClassName);
        updateRequestDtoClassName = safe(updateRequestDtoClassName);
        patchRequestDtoClassName = safe(patchRequestDtoClassName);
        responseDtoClassName = safe(responseDtoClassName);
        pathParameterName = pathParameterName == null || pathParameterName.isBlank()
                ? "id"
                : pathParameterName.trim();
        idJsonPath = idJsonPath == null || idJsonPath.isBlank() ? "id" : idJsonPath.trim();
    }

    public boolean complete() {
        return !className.isBlank()
                && !clientClassName.isBlank()
                && !createMethodName.isBlank()
                && !readMethodName.isBlank()
                && !updateMethodName.isBlank()
                && !patchMethodName.isBlank()
                && !deleteMethodName.isBlank()
                && !createRequestDtoClassName.isBlank()
                && !updateRequestDtoClassName.isBlank()
                && !patchRequestDtoClassName.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
