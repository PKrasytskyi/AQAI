package ua.demo.agentlab.api.spec;

import java.util.List;

public record ApiGenerationSpec(
        List<ApiClientSpec> clientSpecs,
        List<ApiDtoSpec> dtoSpecs,
        List<ApiTestSpec> testSpecs,
        List<ApiCrudScenarioSpec> crudScenarioSpecs
) {
    public ApiGenerationSpec(
            List<ApiClientSpec> clientSpecs,
            List<ApiDtoSpec> dtoSpecs,
            List<ApiTestSpec> testSpecs
    ) {
        this(clientSpecs, dtoSpecs, testSpecs, List.of());
    }

    public ApiGenerationSpec {
        clientSpecs = clientSpecs == null ? List.of() : List.copyOf(clientSpecs);
        dtoSpecs = dtoSpecs == null ? List.of() : List.copyOf(dtoSpecs);
        testSpecs = testSpecs == null ? List.of() : List.copyOf(testSpecs);
        crudScenarioSpecs = crudScenarioSpecs == null ? List.of() : List.copyOf(crudScenarioSpecs);
    }
}
