package ua.demo.agentlab.api.quality;

import ua.demo.agentlab.api.model.ApiAssertionContract;
import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.ApiEndpointModel;
import ua.demo.agentlab.api.model.CanonicalApiTestCase;
import ua.demo.agentlab.api.model.CanonicalApiTestCaseBundle;
import ua.demo.agentlab.api.spec.ApiClientMethodSpec;
import ua.demo.agentlab.api.spec.ApiClientSpec;
import ua.demo.agentlab.api.spec.ApiCrudScenarioSpec;
import ua.demo.agentlab.api.spec.ApiDtoSpec;
import ua.demo.agentlab.api.spec.ApiGenerationSpec;
import ua.demo.agentlab.api.spec.ApiTestSpec;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApiQualityGate {

    public ApiQualityReport validate(
            ApiEndpointBundle endpoints,
            CanonicalApiTestCaseBundle testCases
    ) {
        List<ApiQualityIssue> issues = new ArrayList<>();
        if (endpoints == null || endpoints.endpoints().isEmpty()) {
            issues.add(blocker("API_ENDPOINT_EVIDENCE_PRESENT",
                    "API generation requires confirmed endpoint evidence",
                    "ApiEndpointBundle is empty"));
        }
        if (testCases == null || testCases.testCases().isEmpty()) {
            issues.add(blocker("CANONICAL_API_TEST_CASES_PRESENT",
                    "API generation requires canonical API test cases",
                    "CanonicalApiTestCaseBundle is empty"));
            return new ApiQualityReport("api-generation", issues);
        }
        for (CanonicalApiTestCase testCase : testCases.testCases()) {
            validateTestCase(endpoints, testCase, issues);
        }
        return new ApiQualityReport("api-generation", issues);
    }

    public ApiQualityReport validate(
            ApiEndpointBundle endpoints,
            CanonicalApiTestCaseBundle testCases,
            ApiGenerationSpec generationSpec,
            List<GeneratedSourceFile> sourceFiles
    ) {
        List<ApiQualityIssue> issues = new ArrayList<>(validate(endpoints, testCases, generationSpec).issues());
        validateGeneratedSourceFiles(generationSpec, sourceFiles, issues);
        return new ApiQualityReport("api-generation", issues);
    }

    public ApiQualityReport validate(
            ApiEndpointBundle endpoints,
            CanonicalApiTestCaseBundle testCases,
            ApiGenerationSpec generationSpec
    ) {
        List<ApiQualityIssue> issues = new ArrayList<>(validate(endpoints, testCases).issues());
        if (generationSpec == null) {
            issues.add(blocker("API_GENERATION_SPEC_PRESENT", "API generation spec is required", "ApiGenerationSpec is null"));
            return new ApiQualityReport("api-generation", issues);
        }
        if (endpoints != null && !endpoints.endpoints().isEmpty() && generationSpec.clientSpecs().isEmpty()) {
            issues.add(blocker("API_CLIENT_SPECS_PRESENT", "Endpoint evidence must produce API client specs", endpoints.source()));
        }
        for (ApiClientSpec client : generationSpec.clientSpecs()) {
            validateClientSpec(client, generationSpec.dtoSpecs(), issues);
        }
        for (ApiTestSpec testSpec : generationSpec.testSpecs()) {
            validateTestSpec(testSpec, generationSpec.clientSpecs(), issues);
        }
        for (ApiCrudScenarioSpec crudSpec : generationSpec.crudScenarioSpecs()) {
            validateCrudScenarioSpec(crudSpec, generationSpec.clientSpecs(), generationSpec.dtoSpecs(), issues);
        }
        return new ApiQualityReport("api-generation", issues);
    }

    private void validateTestCase(
            ApiEndpointBundle endpoints,
            CanonicalApiTestCase testCase,
            List<ApiQualityIssue> issues
    ) {
        if (testCase.id().isBlank()) {
            issues.add(blocker("API_TEST_CASE_ID_PRESENT", "API test case id is required", testCase.toString()));
        }
        if (testCase.requirementId().isBlank()) {
            issues.add(blocker("API_REQUIREMENT_TRACE_PRESENT", "API test case must trace to a requirement", testCase.id()));
        }
        if (testCase.endpointId().isBlank()) {
            issues.add(blocker("API_ENDPOINT_ID_PRESENT", "API test case must reference an endpoint id", testCase.id()));
            return;
        }
        Optional<ApiEndpointModel> endpoint = endpoints == null ? Optional.empty() : endpoints.find(testCase.endpointId());
        if (endpoint.isEmpty()) {
            issues.add(blocker("API_ENDPOINT_EXISTS", "Referenced endpoint does not exist in endpoint bundle", testCase.endpointId()));
            return;
        }
        ApiEndpointModel endpointModel = endpoint.get();
        if (!endpointModel.confirmed()) {
            issues.add(blocker("API_ENDPOINT_CONFIRMED", "Endpoint must be confirmed by evidence before generation", endpointModel.endpointId()));
        }
        if (testCase.method() != endpointModel.method()) {
            issues.add(blocker("API_METHOD_MATCHES_ENDPOINT", "Test case HTTP method must match endpoint model",
                    testCase.id() + " -> " + testCase.method() + " vs " + endpointModel.method()));
        }
        if (!testCase.path().equals(endpointModel.path())) {
            issues.add(blocker("API_PATH_MATCHES_ENDPOINT", "Test case path must match endpoint model",
                    testCase.id() + " -> " + testCase.path() + " vs " + endpointModel.path()));
        }
        validateAssertionContract(testCase, endpointModel, issues);
    }

    private void validateAssertionContract(
            CanonicalApiTestCase testCase,
            ApiEndpointModel endpoint,
            List<ApiQualityIssue> issues
    ) {
        ApiAssertionContract contract = testCase.assertionContract();
        if (contract == null) {
            issues.add(blocker("API_ASSERTION_CONTRACT_PRESENT", "API test case requires typed assertion contract", testCase.id()));
            return;
        }
        if (contract.expectedStatusCode() <= 0) {
            issues.add(blocker("API_STATUS_ASSERTION_PRESENT", "API test case requires explicit expected status code", testCase.id()));
        }
        if (!contract.endpointId().equals(testCase.endpointId())) {
            issues.add(blocker("API_ASSERTION_ENDPOINT_MATCHES_TEST", "Assertion contract endpoint must match test case endpoint",
                    testCase.id() + " -> " + contract.endpointId() + " vs " + testCase.endpointId()));
        }
        if (contract.expectedStatusCode() > 0 && !endpoint.hasResponseStatus(contract.expectedStatusCode())) {
            issues.add(warning("API_STATUS_DECLARED_IN_ENDPOINT",
                    "Expected status code is not declared in endpoint evidence",
                    endpoint.endpointId() + " -> " + contract.expectedStatusCode()));
        }
        if (contract.expectedStatusCode() >= 200
                && contract.expectedStatusCode() < 300
                && !contract.hasBodyOrSchemaAssertion()) {
            issues.add(blocker("API_SUCCESS_ASSERTS_BODY_OR_SCHEMA",
                    "Successful API test requires body or schema assertion",
                    testCase.id()));
        }
    }

    private void validateClientSpec(
            ApiClientSpec client,
            List<ApiDtoSpec> dtoSpecs,
            List<ApiQualityIssue> issues
    ) {
        if (client.className().isBlank()) {
            issues.add(blocker("API_CLIENT_CLASS_PRESENT", "API client class name is required", client.toString()));
        }
        if (client.endpointPath().isBlank()) {
            issues.add(blocker("API_CLIENT_ENDPOINT_PRESENT", "API client endpoint path is required", client.className()));
        }
        for (ApiClientMethodSpec method : client.methods()) {
            if (method.methodName().isBlank()) {
                issues.add(blocker("API_CLIENT_METHOD_NAME_PRESENT", "API client method name is required", client.className()));
            }
            if (method.hasRequestBody() && !dtoExists(dtoSpecs, method.requestDtoClassName())) {
                issues.add(blocker("API_REQUEST_DTO_EXISTS", "Client request DTO must exist",
                        client.className() + "." + method.methodName() + " -> " + method.requestDtoClassName()));
            }
        }
    }

    private void validateTestSpec(
            ApiTestSpec testSpec,
            List<ApiClientSpec> clients,
            List<ApiQualityIssue> issues
    ) {
        if (testSpec.clientClassName().isBlank() || testSpec.clientMethodName().isBlank()) {
            issues.add(blocker("API_TEST_USES_CLIENT_METHOD", "API test must use a generated API client method", testSpec.className()));
            return;
        }
        Optional<ApiClientMethodSpec> method = clients.stream()
                .filter(client -> client.className().equals(testSpec.clientClassName()))
                .flatMap(client -> client.methods().stream())
                .filter(candidate -> candidate.methodName().equals(testSpec.clientMethodName()))
                .findFirst();
        if (method.isEmpty()) {
            issues.add(blocker("API_TEST_CLIENT_METHOD_EXISTS", "API test references missing client method",
                    testSpec.className() + " -> " + testSpec.clientClassName() + "." + testSpec.clientMethodName()));
        } else if (!method.get().pathParameters().isEmpty()) {
            issues.add(blocker("API_TEST_PATH_PARAMS_REQUIRE_SCENARIO_DATA",
                    "Generated API tests with path parameters require explicit scenario data",
                    testSpec.className() + " -> " + method.get().pathTemplate()));
        } else if (method.get().httpMethod() != ua.demo.agentlab.api.model.HttpMethod.GET) {
            issues.add(blocker("API_MUTATION_TESTS_REQUIRE_DATA_AND_AUTH_POLICY",
                    "Generated mutation API tests require explicit data factory and authentication policy",
                    testSpec.className() + " -> " + method.get().httpMethod() + " " + method.get().pathTemplate()));
        }
        if (testSpec.assertionContract() == null) {
            issues.add(blocker("API_TEST_ASSERTION_CONTRACT_PRESENT", "API test spec requires assertion contract", testSpec.className()));
        }
    }

    private void validateCrudScenarioSpec(
            ApiCrudScenarioSpec crudSpec,
            List<ApiClientSpec> clients,
            List<ApiDtoSpec> dtoSpecs,
            List<ApiQualityIssue> issues
    ) {
        if (crudSpec == null || !crudSpec.complete()) {
            issues.add(blocker("API_CRUD_SCENARIO_COMPLETE",
                    "CRUD scenario requires create/read/update/patch/delete methods and DTOs",
                    String.valueOf(crudSpec)));
            return;
        }
        Optional<ApiClientSpec> client = clients.stream()
                .filter(candidate -> candidate.className().equals(crudSpec.clientClassName()))
                .findFirst();
        if (client.isEmpty()) {
            issues.add(blocker("API_CRUD_CLIENT_EXISTS",
                    "CRUD scenario references missing client",
                    crudSpec.className() + " -> " + crudSpec.clientClassName()));
            return;
        }
        requireCrudMethod(client.get(), crudSpec.createMethodName(), ua.demo.agentlab.api.model.HttpMethod.POST, false, true, issues);
        requireCrudMethod(client.get(), crudSpec.readMethodName(), ua.demo.agentlab.api.model.HttpMethod.GET, true, false, issues);
        requireCrudMethod(client.get(), crudSpec.updateMethodName(), ua.demo.agentlab.api.model.HttpMethod.PUT, true, true, issues);
        requireCrudMethod(client.get(), crudSpec.patchMethodName(), ua.demo.agentlab.api.model.HttpMethod.PATCH, true, true, issues);
        requireCrudMethod(client.get(), crudSpec.deleteMethodName(), ua.demo.agentlab.api.model.HttpMethod.DELETE, true, false, issues);
        requireDto(dtoSpecs, crudSpec.createRequestDtoClassName(), crudSpec.className(), issues);
        requireDto(dtoSpecs, crudSpec.updateRequestDtoClassName(), crudSpec.className(), issues);
        requireDto(dtoSpecs, crudSpec.patchRequestDtoClassName(), crudSpec.className(), issues);
    }

    private void requireCrudMethod(
            ApiClientSpec client,
            String methodName,
            ua.demo.agentlab.api.model.HttpMethod httpMethod,
            boolean requiresPathParameter,
            boolean requiresBody,
            List<ApiQualityIssue> issues
    ) {
        Optional<ApiClientMethodSpec> method = client.methods().stream()
                .filter(candidate -> candidate.methodName().equals(methodName))
                .findFirst();
        if (method.isEmpty()) {
            issues.add(blocker("API_CRUD_CLIENT_METHOD_EXISTS",
                    "CRUD scenario references missing client method",
                    client.className() + "." + methodName));
            return;
        }
        ApiClientMethodSpec spec = method.get();
        if (spec.httpMethod() != httpMethod) {
            issues.add(blocker("API_CRUD_METHOD_MATCHES_HTTP_VERB",
                    "CRUD method uses unexpected HTTP verb",
                    methodName + " -> " + spec.httpMethod() + " vs " + httpMethod));
        }
        if (requiresPathParameter && spec.pathParameters().isEmpty()) {
            issues.add(blocker("API_CRUD_PATH_PARAMETER_PRESENT",
                    "CRUD read/update/patch/delete methods require a path parameter",
                    client.className() + "." + methodName));
        }
        if (requiresBody && !spec.hasRequestBody()) {
            issues.add(blocker("API_CRUD_REQUEST_BODY_PRESENT",
                    "CRUD create/update/patch methods require request DTOs",
                    client.className() + "." + methodName));
        }
    }

    private void requireDto(
            List<ApiDtoSpec> dtoSpecs,
            String className,
            String evidence,
            List<ApiQualityIssue> issues
    ) {
        if (!dtoExists(dtoSpecs, className)) {
            issues.add(blocker("API_CRUD_REQUEST_DTO_EXISTS",
                    "CRUD scenario request DTO must exist",
                    evidence + " -> " + className));
        }
    }

    private void validateGeneratedSourceFiles(
            ApiGenerationSpec generationSpec,
            List<GeneratedSourceFile> sourceFiles,
            List<ApiQualityIssue> issues
    ) {
        if (generationSpec == null || generationSpec.clientSpecs().isEmpty()) {
            return;
        }
        if (sourceFiles == null || sourceFiles.isEmpty()) {
            issues.add(blocker("API_GENERATED_SOURCES_PRESENT",
                    "Quality-passed API generation must produce source files",
                    "Generated source file list is empty"));
            return;
        }
        for (GeneratedSourceFile sourceFile : sourceFiles) {
            if (sourceFile.content() == null || sourceFile.content().isBlank()) {
                issues.add(blocker("API_GENERATED_SOURCE_CONTENT_PRESENT",
                        "Generated API source file must not be blank",
                        sourceFile.relativePath()));
            }
            if (sourceFile.content() != null && sourceFile.content().contains("import ;")) {
                issues.add(blocker("API_GENERATED_SOURCE_IMPORTS_VALID",
                        "Generated API source file must not contain blank imports",
                        sourceFile.relativePath()));
            }
            validateSourceRoot(sourceFile, issues);
        }
    }

    private void validateSourceRoot(GeneratedSourceFile sourceFile, List<ApiQualityIssue> issues) {
        String relativePath = sourceFile.relativePath() == null ? "" : sourceFile.relativePath();
        String packageName = sourceFile.packageName() == null ? "" : sourceFile.packageName();
        if (packageName.contains(".generated.tests")) {
            if (!relativePath.startsWith("src/test/java/")) {
                issues.add(blocker("API_GENERATED_TEST_SOURCE_ROOT",
                        "Generated API tests must be written under src/test/java",
                        relativePath));
            }
            return;
        }
        if (packageName.contains(".generated.clients") || packageName.contains(".generated.models")) {
            if (!relativePath.startsWith("src/main/java/")) {
                issues.add(blocker("API_GENERATED_SUPPORT_SOURCE_ROOT",
                        "Generated API clients and DTOs must be written under src/main/java",
                        relativePath));
            }
        }
    }

    private boolean dtoExists(List<ApiDtoSpec> dtoSpecs, String className) {
        if (className == null || className.isBlank()) {
            return true;
        }
        return dtoSpecs.stream().anyMatch(dto -> className.equals(dto.className()));
    }

    private ApiQualityIssue blocker(String ruleId, String message, String evidence) {
        return new ApiQualityIssue(ApiQualitySeverity.BLOCKER, ruleId, message, evidence);
    }

    private ApiQualityIssue warning(String ruleId, String message, String evidence) {
        return new ApiQualityIssue(ApiQualitySeverity.WARNING, ruleId, message, evidence);
    }
}
