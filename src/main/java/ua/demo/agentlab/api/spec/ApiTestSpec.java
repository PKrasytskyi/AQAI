package ua.demo.agentlab.api.spec;

import ua.demo.agentlab.api.model.ApiAssertionContract;

public record ApiTestSpec(
        String packageName,
        String className,
        String testMethodName,
        String clientClassName,
        String clientMethodName,
        String requestDtoClassName,
        String responseDtoClassName,
        String dataSetName,
        ApiAssertionContract assertionContract
) {
    public ApiTestSpec {
        packageName = packageName == null || packageName.isBlank()
                ? "ua.demo.agentlab.api.generated.tests"
                : packageName.trim();
        className = className == null ? "" : className.trim();
        testMethodName = testMethodName == null ? "" : testMethodName.trim();
        clientClassName = clientClassName == null ? "" : clientClassName.trim();
        clientMethodName = clientMethodName == null ? "" : clientMethodName.trim();
        requestDtoClassName = requestDtoClassName == null ? "" : requestDtoClassName.trim();
        responseDtoClassName = responseDtoClassName == null ? "" : responseDtoClassName.trim();
        dataSetName = dataSetName == null ? "" : dataSetName.trim();
    }
}
