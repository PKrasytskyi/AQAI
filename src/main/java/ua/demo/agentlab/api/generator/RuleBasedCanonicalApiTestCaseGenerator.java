package ua.demo.agentlab.api.generator;

import ua.demo.agentlab.api.model.ApiAssertionContract;
import ua.demo.agentlab.api.model.ApiAssertionType;
import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.ApiEndpointModel;
import ua.demo.agentlab.api.model.ApiOperationKind;
import ua.demo.agentlab.api.model.ApiResponseModel;
import ua.demo.agentlab.api.model.CanonicalApiTestCase;
import ua.demo.agentlab.api.model.CanonicalApiTestCaseBundle;
import ua.demo.agentlab.api.model.HttpMethod;
import ua.demo.agentlab.api.model.JsonPathAssertion;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class RuleBasedCanonicalApiTestCaseGenerator {

    public CanonicalApiTestCaseBundle generate(ApiEndpointBundle endpoints) {
        if (endpoints == null || endpoints.endpoints().isEmpty()) {
            return new CanonicalApiTestCaseBundle("api-endpoint-bundle", List.of());
        }
        AtomicInteger index = new AtomicInteger(1);
        List<CanonicalApiTestCase> testCases = endpoints.endpoints().stream()
                .filter(endpoint -> endpoint.method() == HttpMethod.GET)
                .filter(endpoint -> !endpoint.path().contains("{"))
                .filter(ApiEndpointModel::confirmed)
                .map(endpoint -> testCase(endpoint, index.getAndIncrement()))
                .toList();
        return new CanonicalApiTestCaseBundle(endpoints.source(), testCases);
    }

    private CanonicalApiTestCase testCase(ApiEndpointModel endpoint, int index) {
        String id = "API-%03d".formatted(index);
        int expectedStatus = endpoint.responses().stream()
                .filter(ApiResponseModel::successful)
                .map(ApiResponseModel::statusCode)
                .findFirst()
                .orElse(200);
        ApiAssertionContract contract = new ApiAssertionContract(
                "REQ-AUTO-" + id,
                id,
                endpoint.endpointId(),
                expectedStatus,
                List.of(new JsonPathAssertion(ApiAssertionType.JSON_FIELD_EXISTS, defaultJsonPath(endpoint), "")),
                List.of(),
                List.of(),
                List.of()
        );
        return new CanonicalApiTestCase(
                id,
                "REQ-AUTO-" + id,
                title(endpoint),
                endpoint.endpointId(),
                endpoint.method(),
                endpoint.path(),
                dataSetName(endpoint),
                ApiOperationKind.READ,
                contract,
                List.of("API is available"),
                List.of(),
                endpoint.evidence().stream().findFirst().map(evidence -> evidence.reference()).orElse("")
        );
    }

    private String title(ApiEndpointModel endpoint) {
        String operation = endpoint.operationName().isBlank() ? endpoint.path() : endpoint.operationName();
        return "Verify " + operation + " returns a successful response";
    }

    private String dataSetName(ApiEndpointModel endpoint) {
        return endpoint.path().toLowerCase(Locale.ROOT)
                .replaceAll("\\{[^}]+}", "id")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "")
                + "-default";
    }

    private String defaultJsonPath(ApiEndpointModel endpoint) {
        return endpoint.path().contains("{") ? "id" : "id";
    }
}
