package ua.demo.agentlab.ai.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LlmOutputSchemaValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmOutputSchemaValidator validator = new LlmOutputSchemaValidator();

    @Test
    public void pageObjectSpecRequiresSchemaVersion() throws Exception {
        var report = validator.validatePageObjectSpec(objectMapper.readTree("""
                {"pageObjects":[]}
                """));

        Assert.assertFalse(report.valid());
        Assert.assertTrue(report.issues().stream().anyMatch(issue -> issue.path().equals("$.schemaVersion")));
    }

    @Test
    public void pageObjectSpecAcceptsValidContract() throws Exception {
        var report = validator.validatePageObjectSpec(objectMapper.readTree("""
                {
                  "schemaVersion": "ai-page-object-spec.v1",
                  "pageObjects": [
                    {
                      "pageName": "LoginPage",
                      "route": "/login",
                      "openMethodName": "openLogin",
                      "locators": [
                        {"fieldName": "usernameInput", "elementName": "username input", "strategy": "id", "value": "username"}
                      ],
                      "methods": [
                        {"returnType": "boolean", "methodName": "isPageOpened", "parameters": [], "body": "return getCurrentUrl().contains(\\"/login\\");", "requiredImports": []}
                      ]
                    }
                  ]
                }
                """));

        Assert.assertTrue(report.valid(), report.issues().toString());
    }

    @Test
    public void uiTestSpecRejectsMissingAssertionBody() throws Exception {
        var report = validator.validateUiTestSpec(objectMapper.readTree("""
                {
                  "schemaVersion": "ai-ui-test-spec.v1",
                  "tests": [
                    {
                      "scenarioId": "REQ-001",
                      "className": "LoginTest",
                      "sourcePageClassName": "LoginPage",
                      "sourcePageVariableName": "login",
                      "pageClassName": "LoginPage",
                      "pageVariableName": "login",
                      "testMethodName": "shouldLogin",
                      "testDescription": "Login",
                      "actionBody": "login.openLogin();",
                      "additionalImports": []
                    }
                  ]
                }
                """));

        Assert.assertFalse(report.valid());
        Assert.assertTrue(report.issues().stream().anyMatch(issue -> issue.path().contains("assertionBody")));
    }
}
