package ua.demo.agentlab.api.generator;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.api.discovery.ApiEndpointSeedParser;
import ua.demo.agentlab.api.model.ApiAssertionContract;
import ua.demo.agentlab.api.model.ApiAssertionType;
import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.ApiOperationKind;
import ua.demo.agentlab.api.model.CanonicalApiTestCase;
import ua.demo.agentlab.api.model.CanonicalApiTestCaseBundle;
import ua.demo.agentlab.api.model.HttpMethod;
import ua.demo.agentlab.api.model.JsonPathAssertion;
import ua.demo.agentlab.api.quality.ApiQualityGate;
import ua.demo.agentlab.api.quality.ApiQualityReport;
import ua.demo.agentlab.api.spec.ApiClientSpec;
import ua.demo.agentlab.api.spec.ApiGenerationSpec;
import ua.demo.agentlab.api.writer.ApiRestAssuredTestNgWriter;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;

public class ApiTemplateSpecGeneratorTest {

    @Test
    public void generatesClientDtosTestsAndPassesApiQualityGate() {
        ApiEndpointBundle endpoints = new ApiEndpointSeedParser().parse(
                "test-seed",
                "GET /public/v2/users listUsers; "
                        + "GET /public/v2/users/{id} getUser; "
                        + "POST /public/v2/users createUser; "
                        + "GET /public/v2/users/{id}/posts listUserPosts; "
                        + "POST /public/v2/users/{id}/posts createUserPost"
        );
        CanonicalApiTestCaseBundle testCases = new CanonicalApiTestCaseBundle(
                "requirements",
                List.of(canonicalGetUsersTestCase())
        );

        ApiGenerationSpec spec = new ApiTemplateSpecGenerator().generate(endpoints, testCases);
        ApiQualityReport report = new ApiQualityGate().validate(endpoints, testCases, spec);
        List<GeneratedSourceFile> files = new ApiRestAssuredTestNgWriter().write(spec);

        Assert.assertFalse(report.hasBlockingIssues(), report.issues().toString());
        Assert.assertTrue(spec.clientSpecs().stream().anyMatch(client -> "UserClient".equals(client.className())));
        Assert.assertTrue(spec.dtoSpecs().stream().anyMatch(dto -> "CreateUserRequest".equals(dto.className())));
        Assert.assertTrue(spec.dtoSpecs().stream().anyMatch(dto -> "UserResponse".equals(dto.className())));
        Assert.assertTrue(spec.testSpecs().stream().anyMatch(test -> "getUsers".equals(test.clientMethodName())));
        Assert.assertEquals(file(files, "UserClient").relativePath(),
                "src/main/java/ua/demo/agentlab/api/generated/clients/UserClient.java");
        Assert.assertEquals(file(files, "CreateUserRequest").relativePath(),
                "src/main/java/ua/demo/agentlab/api/generated/models/request/CreateUserRequest.java");

        String clientContent = file(files, "UserClient").content();
        Assert.assertTrue(clientContent.contains("public Response getUsers()"));
        Assert.assertTrue(clientContent.contains("public Response getUserById(int id)"));
        Assert.assertTrue(clientContent.contains("public Response listUserPosts(int id)"));
        Assert.assertTrue(clientContent.contains("public Response createUserPost(int id, CreatePostRequest bodyRequest)"));
        Assert.assertTrue(clientContent.contains(".pathParam(\"id\", id)"));
        Assert.assertTrue(clientContent.contains("public Response createUser(CreateUserRequest bodyRequest)"));
        Assert.assertTrue(clientContent.contains("public Response createUserWithoutAuthorization(CreateUserRequest bodyRequest)"));

        String dtoContent = file(files, "CreateUserRequest").content();
        Assert.assertTrue(dtoContent.contains("private String email;"));
        Assert.assertTrue(dtoContent.contains("public void setStatus(String status)"));
        Assert.assertTrue(file(files, "CreatePostRequest").content().contains("private String title;"));
    }

    @Test
    public void generatesFullCrudScenarioWhenResourceHasCreateReadUpdatePatchDelete() {
        ApiEndpointBundle endpoints = new ApiEndpointSeedParser().parse(
                "test-seed",
                "GET /public/v2/users listUsers; "
                        + "GET /public/v2/users/{id} getUser; "
                        + "POST /public/v2/users createUser; "
                        + "PUT /public/v2/users/{id} updateUser; "
                        + "PATCH /public/v2/users/{id} patchUser; "
                        + "DELETE /public/v2/users/{id} deleteUser"
        );
        CanonicalApiTestCaseBundle testCases = new RuleBasedCanonicalApiTestCaseGenerator().generate(endpoints);

        ApiGenerationSpec spec = new ApiTemplateSpecGenerator().generate(endpoints, testCases);
        ApiQualityReport report = new ApiQualityGate().validate(endpoints, testCases, spec);
        List<GeneratedSourceFile> files = new ApiRestAssuredTestNgWriter().write(spec);

        Assert.assertFalse(report.hasBlockingIssues(), report.issues().toString());
        Assert.assertEquals(spec.crudScenarioSpecs().size(), 1);
        String crudContent = file(files, "UserCrudApiTest").content();
        Assert.assertTrue(crudContent.contains("client.createUser(createRequest)"));
        Assert.assertTrue(crudContent.contains("client.getUserById(createdId)"));
        Assert.assertTrue(crudContent.contains("client.updateUserById(createdId, updateRequest)"));
        Assert.assertTrue(crudContent.contains("client.patchUserById(createdId, patchRequest)"));
        Assert.assertTrue(crudContent.contains("client.deleteUserById(createdId)"));
        Assert.assertTrue(crudContent.contains("ApiAssertions.assertStatusCode(deleteResponse.statusCode(), 204);"));
    }

    @Test
    public void defaultCanonicalApiTestsUseOnlyConfirmedGetCollectionEndpoints() {
        ApiEndpointBundle endpoints = new ApiEndpointSeedParser().parse(
                "test-seed",
                "GET /public/v2/users listUsers; "
                        + "GET /public/v2/users/{id} getUser; "
                        + "POST /public/v2/users createUser"
        );

        CanonicalApiTestCaseBundle testCases = new RuleBasedCanonicalApiTestCaseGenerator().generate(endpoints);

        Assert.assertEquals(testCases.testCases().size(), 1);
        Assert.assertEquals(testCases.testCases().get(0).endpointId(), "GET:/public/v2/users");
    }

    private CanonicalApiTestCase canonicalGetUsersTestCase() {
        ApiAssertionContract contract = new ApiAssertionContract(
                "REQ-API-001",
                "API-001",
                "GET:/public/v2/users",
                200,
                List.of(new JsonPathAssertion(ApiAssertionType.JSON_FIELD_EXISTS, "id", "")),
                List.of(),
                List.of(),
                List.of()
        );
        return new CanonicalApiTestCase(
                "API-001",
                "REQ-API-001",
                "Get users returns user records",
                "GET:/public/v2/users",
                HttpMethod.GET,
                "/public/v2/users",
                "users-default",
                ApiOperationKind.READ,
                contract,
                List.of("API is available"),
                List.of(),
                "requirements/api.md [L1]"
        );
    }

    private GeneratedSourceFile file(List<GeneratedSourceFile> files, String className) {
        return files.stream()
                .filter(file -> className.equals(file.className()))
                .findFirst()
                .orElseThrow();
    }
}
