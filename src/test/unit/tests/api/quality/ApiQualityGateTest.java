package unit.tests.api.quality;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.api.model.ApiAssertionContract;
import ua.demo.agentlab.api.model.ApiAuthMode;
import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.ApiEndpointEvidence;
import ua.demo.agentlab.api.model.ApiEndpointEvidenceSource;
import ua.demo.agentlab.api.model.ApiEndpointModel;
import ua.demo.agentlab.api.model.ApiOperationKind;
import ua.demo.agentlab.api.model.ApiResponseModel;
import ua.demo.agentlab.api.model.CanonicalApiTestCase;
import ua.demo.agentlab.api.model.CanonicalApiTestCaseBundle;
import ua.demo.agentlab.api.model.HttpMethod;
import ua.demo.agentlab.api.model.JsonPathAssertion;
import ua.demo.agentlab.api.spec.ApiClientMethodSpec;
import ua.demo.agentlab.api.spec.ApiClientSpec;
import ua.demo.agentlab.api.spec.ApiGenerationSpec;
import ua.demo.agentlab.api.spec.ApiTestSpec;
import ua.demo.agentlab.api.quality.ApiQualityGate;
import ua.demo.agentlab.api.quality.ApiQualityReport;

import java.util.List;

public class ApiQualityGateTest {

    private final ApiQualityGate gate = new ApiQualityGate();

    @Test
    public void confirmedEndpointWithTypedAssertionContractPassesGate() {
        ApiEndpointBundle endpoints = new ApiEndpointBundle(
                "openapi",
                List.of(endpoint(0.91d))
        );
        CanonicalApiTestCaseBundle testCases = new CanonicalApiTestCaseBundle(
                "requirements",
                List.of(testCase(contractWithBodyAssertion()))
        );

        ApiQualityReport report = gate.validate(endpoints, testCases);

        Assert.assertFalse(report.hasBlockingIssues(), report.issues().toString());
    }

    @Test
    public void unconfirmedEndpointBlocksGeneration() {
        ApiEndpointBundle endpoints = new ApiEndpointBundle(
                "network-scan",
                List.of(endpoint(0.30d))
        );
        CanonicalApiTestCaseBundle testCases = new CanonicalApiTestCaseBundle(
                "requirements",
                List.of(testCase(contractWithBodyAssertion()))
        );

        ApiQualityReport report = gate.validate(endpoints, testCases);

        Assert.assertTrue(hasRule(report, "API_ENDPOINT_CONFIRMED"));
    }

    @Test
    public void successfulApiTestRequiresBodyOrSchemaAssertion() {
        ApiAssertionContract weakContract = new ApiAssertionContract(
                "REQ-API-001",
                "API-001",
                "GET:/employees",
                200,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        ApiEndpointBundle endpoints = new ApiEndpointBundle(
                "openapi",
                List.of(endpoint(0.91d))
        );
        CanonicalApiTestCaseBundle testCases = new CanonicalApiTestCaseBundle(
                "requirements",
                List.of(testCase(weakContract))
        );

        ApiQualityReport report = gate.validate(endpoints, testCases);

        Assert.assertTrue(hasRule(report, "API_SUCCESS_ASSERTS_BODY_OR_SCHEMA"));
    }

    @Test
    public void generatedApiTestsWithPathParametersRequireScenarioData() {
        ApiEndpointBundle endpoints = new ApiEndpointBundle(
                "openapi",
                List.of(endpoint(0.91d))
        );
        CanonicalApiTestCaseBundle testCases = new CanonicalApiTestCaseBundle(
                "requirements",
                List.of(testCase(contractWithBodyAssertion()))
        );
        ApiGenerationSpec spec = new ApiGenerationSpec(
                List.of(new ApiClientSpec(
                        "ua.demo.agentlab.api.generated.clients",
                        "EmployeeClient",
                        "EMPLOYEES_ENDPOINT",
                        "/employees",
                        List.of(new ApiClientMethodSpec(
                                "getEmployeeById",
                                HttpMethod.GET,
                                "/employees/{id}",
                                "",
                                "EmployeeResponse",
                                ApiAuthMode.NONE,
                                false,
                                List.of("id")
                        ))
                )),
                List.of(),
                List.of(new ApiTestSpec(
                        "ua.demo.agentlab.api.generated.tests",
                        "GetEmployeeByIdApiTest",
                        "shouldGetEmployeeById",
                        "EmployeeClient",
                        "getEmployeeById",
                        "",
                        "EmployeeResponse",
                        "employee-by-id-default",
                        contractWithBodyAssertion()
                ))
        );

        ApiQualityReport report = gate.validate(endpoints, testCases, spec);

        Assert.assertTrue(hasRule(report, "API_TEST_PATH_PARAMS_REQUIRE_SCENARIO_DATA"));
    }

    @Test
    public void generatedMutationApiTestsRequireDataFactoryAndAuthPolicy() {
        ApiEndpointBundle endpoints = new ApiEndpointBundle(
                "openapi",
                List.of(endpoint(0.91d))
        );
        CanonicalApiTestCaseBundle testCases = new CanonicalApiTestCaseBundle(
                "requirements",
                List.of(testCase(contractWithBodyAssertion()))
        );
        ApiGenerationSpec spec = new ApiGenerationSpec(
                List.of(new ApiClientSpec(
                        "ua.demo.agentlab.api.generated.clients",
                        "EmployeeClient",
                        "EMPLOYEES_ENDPOINT",
                        "/employees",
                        List.of(new ApiClientMethodSpec(
                                "createEmployee",
                                HttpMethod.POST,
                                "/employees",
                                "CreateEmployeeRequest",
                                "EmployeeResponse",
                                ApiAuthMode.AUTHORIZED,
                                false,
                                List.of()
                        ))
                )),
                List.of(),
                List.of(new ApiTestSpec(
                        "ua.demo.agentlab.api.generated.tests",
                        "CreateEmployeeApiTest",
                        "shouldCreateEmployee",
                        "EmployeeClient",
                        "createEmployee",
                        "CreateEmployeeRequest",
                        "EmployeeResponse",
                        "employee-create-default",
                        contractWithBodyAssertion()
                ))
        );

        ApiQualityReport report = gate.validate(endpoints, testCases, spec);

        Assert.assertTrue(hasRule(report, "API_MUTATION_TESTS_REQUIRE_DATA_AND_AUTH_POLICY"));
    }

    private ApiEndpointModel endpoint(double confidence) {
        return new ApiEndpointModel(
                "GET:/employees",
                HttpMethod.GET,
                "/employees",
                "getEmployees",
                "employee-list",
                List.of(),
                null,
                List.of(new ApiResponseModel(200, "application/json", "EmployeeListResponse", List.of())),
                List.of("admin"),
                List.of(new ApiEndpointEvidence(ApiEndpointEvidenceSource.OPENAPI, "openapi.yaml#/paths/~1employees/get", confidence)),
                confidence
        );
    }

    private CanonicalApiTestCase testCase(ApiAssertionContract contract) {
        return new CanonicalApiTestCase(
                "API-001",
                "REQ-API-001",
                "Employee list is returned",
                "GET:/employees",
                HttpMethod.GET,
                "/employees",
                "employee-list-default",
                ApiOperationKind.READ,
                contract,
                List.of("Application is available"),
                List.of(),
                "requirements/api.md [L10]"
        );
    }

    private ApiAssertionContract contractWithBodyAssertion() {
        return new ApiAssertionContract(
                "REQ-API-001",
                "API-001",
                "GET:/employees",
                200,
                List.of(new JsonPathAssertion(null, "data", "")),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private boolean hasRule(ApiQualityReport report, String ruleId) {
        return report.issues().stream().anyMatch(issue -> ruleId.equals(issue.ruleId()));
    }
}
