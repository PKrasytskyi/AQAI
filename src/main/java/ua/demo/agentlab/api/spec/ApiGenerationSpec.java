package ua.demo.agentlab.api.spec;

import java.util.List;

public record ApiGenerationSpec(
        List<ApiClientSpec> clientSpecs,
        List<ApiDtoSpec> dtoSpecs,
        List<ApiTestSpec> testSpecs
) {
    public ApiGenerationSpec {
        clientSpecs = clientSpecs == null ? List.of() : List.copyOf(clientSpecs);
        dtoSpecs = dtoSpecs == null ? List.of() : List.copyOf(dtoSpecs);
        testSpecs = testSpecs == null ? List.of() : List.copyOf(testSpecs);
    }
}
