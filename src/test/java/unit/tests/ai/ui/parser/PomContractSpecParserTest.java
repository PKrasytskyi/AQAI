package unit.tests.ai.ui.parser;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;
import ua.demo.agentlab.ai.ui.parser.PomContractSpecParser;

public class PomContractSpecParserTest {

    @Test
    public void repairsCommonLlmAliasesBeforeSchemaValidation() {
        PomContractSpec contract = new PomContractSpecParser().parse("""
                {
                  "schemaVersion": "pom-contract-v1",
                  "page": {"name": "LoginPage", "route": "/auth/login", "capability": "AUTHENTICATION", "openMethod": "openLogin"},
                  "locators": [
                    {"id": "usernameInput", "elementName": "username", "strategy": "name", "value": "username", "role": "input", "stabilityScore": 0.87},
                    {"id": "loginButton", "elementName": "login", "strategy": "css", "value": "button[type='submit']", "role": "button", "stabilityScore": 0.87}
                  ],
                  "actions": [
                    {
                      "methodName": "enterUsername",
                      "kind": "ACTION",
                      "parameters": [{"type": "String", "name": "username"}],
                      "steps": [{"action": "TYPE", "locator": "usernameInput", "valueFrom": "parameter", "literalValue": "", "route": ""}]
                    },
                    {
                      "methodName": "logout",
                      "kind": "ACTION",
                      "parameters": [],
                      "steps": [{"action": "COVERAGE_GAP", "locator": "", "valueFrom": "", "literalValue": "", "route": ""}]
                    }
                  ],
                  "assertions": [
                    {
                      "methodName": "isLoginFormVisible",
                      "returnType": "boolean",
                      "checks": [{"check": "FORM_VISIBLE", "locator": "loginButton", "expectedValue": "", "valueFrom": "", "attribute": "", "route": ""}],
                      "combine": "AND"
                    }
                  ],
                  "coverageGaps": [{"id": "CG-1", "area": "missing form root", "detail": "No form locator"}],
                  "rejectedSuggestions": []
                }
                """);

        Assert.assertEquals(contract.actions().size(), 1);
        Assert.assertEquals(contract.actions().get(0).steps().get(0).action(), PomStepAction.CLEAR_AND_TYPE);
        Assert.assertEquals(contract.actions().get(0).steps().get(0).valueFrom(), "username");
        Assert.assertEquals(contract.assertions().get(0).checks().get(0).check(), PomCheckType.VISIBLE);
        Assert.assertTrue(contract.coverageGaps().stream().anyMatch(gap -> gap.contains("CG-1")));
        Assert.assertFalse(contract.coverageGaps().stream().anyMatch(gap -> gap.contains("logout")));
    }

    @Test
    public void infersBlankInputStepValueFromDeclaredParameters() {
        PomContractSpec contract = new PomContractSpecParser().parse("""
                {
                  "schemaVersion": "pom-contract-v1",
                  "page": {"name": "LoginPage", "route": "/auth/login", "capability": "AUTHENTICATION", "openMethod": "openLogin"},
                  "locators": [
                    {"id": "usernameInput", "elementName": "username", "strategy": "name", "value": "username", "role": "input", "stabilityScore": 0.87},
                    {"id": "passwordInput", "elementName": "password", "strategy": "name", "value": "password", "role": "password", "stabilityScore": 0.87},
                    {"id": "loginButton", "elementName": "login", "strategy": "css", "value": "button[type='submit']", "role": "button", "stabilityScore": 0.87}
                  ],
                  "components": [],
                  "actions": [
                    {
                      "methodName": "login",
                      "kind": "ACTION",
                      "parameters": [{"type": "String", "name": "username"}, {"type": "String", "name": "password"}],
                      "steps": [
                        {"action": "CLEAR_AND_TYPE", "locator": "usernameInput", "valueFrom": "", "literalValue": "", "route": ""},
                        {"action": "CLEAR_AND_TYPE", "locator": "passwordInput", "valueFrom": "", "literalValue": "", "route": ""},
                        {"action": "CLICK", "locator": "loginButton", "valueFrom": "", "literalValue": "", "route": ""}
                      ]
                    }
                  ],
                  "assertions": [],
                  "coverageGaps": [],
                  "rejectedSuggestions": []
                }
                """);

        Assert.assertEquals(contract.actions().get(0).steps().get(0).valueFrom(), "username");
        Assert.assertEquals(contract.actions().get(0).steps().get(1).valueFrom(), "password");
    }


    @Test
    public void repairsCompactLocatorMetadataAndDropsMalformedComponents() {
        PomContractSpec contract = new PomContractSpecParser().parse("""
                {
                  "schemaVersion": "pom-contract-v1",
                  "page": {"name": "LoginPage", "route": "/auth/login", "capability": "AUTHENTICATION", "openMethod": "openLogin"},
                  "locators": [
                    {"id": "usernameInput", "component": "Form1Component", "strategy": "name", "value": "username"},
                    {"id": "loginButton", "component": "Form1Component", "strategy": "css", "value": "button[type='submit']"}
                  ],
                  "components": [
                    {
                      "id": "Form1Component",
                      "type": "FORM",
                      "rootLocator": {"strategy": "css", "value": "form"},
                      "ownedLocators": ["usernameInput", "loginButton"]
                    }
                  ],
                  "actions": [
                    {
                      "methodName": "clickLoginButton",
                      "kind": "ACTION",
                      "parameters": [],
                      "steps": [{"action": "CLICK", "locator": "loginButton", "valueFrom": "", "literalValue": "", "route": ""}]
                    }
                  ],
                  "assertions": [],
                  "coverageGaps": [],
                  "rejectedSuggestions": []
                }
                """);

        Assert.assertEquals(contract.locators().size(), 2);
        Assert.assertEquals(contract.locators().get(0).elementName(), "usernameInput");
        Assert.assertEquals(contract.locators().get(0).role(), "input");
        Assert.assertEquals(contract.locators().get(0).stabilityScore(), 0.75d);
        Assert.assertTrue(contract.components().isEmpty());
    }

    @Test
    public void repairsLlmLocatorElementAliasBeforeRecordDeserialization() {
        PomContractSpec contract = new PomContractSpecParser().parse("""
                {
                  "schemaVersion": "pom-contract-v1",
                  "page": {"name": "LoginPage", "route": "/auth/login", "capability": "AUTHENTICATION", "openMethod": "openLogin"},
                  "locators": [
                    {"id": "usernameInput", "component": "Form1Component", "element": "username", "strategy": "name", "value": "username"},
                    {"id": "passwordInput", "component": "Form1Component", "element": "password", "strategy": "name", "value": "password"},
                    {"id": "loginButton", "component": "Form1Component", "element": "login", "strategy": "css", "value": "button[type='submit']"}
                  ],
                  "components": [
                    {
                      "id": "Form1Component",
                      "type": "FORM",
                      "rootLocator": {"strategy": "css", "value": "form"},
                      "locators": ["usernameInput", "passwordInput", "loginButton"]
                    }
                  ],
                  "actions": [
                    {
                      "methodName": "login",
                      "kind": "ACTION",
                      "parameters": [{"type": "String", "name": "username"}, {"type": "String", "name": "password"}],
                      "steps": [
                        {"action": "CLEAR_AND_TYPE", "locator": "usernameInput", "valueFrom": "username", "literalValue": "", "route": ""},
                        {"action": "CLEAR_AND_TYPE", "locator": "passwordInput", "valueFrom": "password", "literalValue": "", "route": ""},
                        {"action": "CLICK", "locator": "loginButton", "valueFrom": "", "literalValue": "", "route": ""}
                      ]
                    }
                  ],
                  "assertions": [
                    {
                      "methodName": "isOnLoginRoute",
                      "returnType": "boolean",
                      "checks": [{"check": "URL_CONTAINS", "locator": "", "expectedValue": "/auth/login", "valueFrom": "", "attribute": "", "route": ""}],
                      "combine": "AND"
                    }
                  ],
                  "coverageGaps": [],
                  "rejectedSuggestions": []
                }
                """);

        Assert.assertEquals(contract.locators().size(), 3);
        Assert.assertEquals(contract.locators().get(0).elementName(), "username");
        Assert.assertEquals(contract.locators().get(1).elementName(), "password");
        Assert.assertEquals(contract.actions().get(0).steps().size(), 3);
        Assert.assertTrue(contract.components().isEmpty());
    }

    @Test
    public void stripsMapperLocatorMetadataBeforeRecordDeserialization() {
        PomContractSpec contract = new PomContractSpecParser().parse("""
                {
                  "schemaVersion": "pom-contract-v1",
                  "page": {"name": "LoginPage", "route": "/auth/login", "capability": "AUTHENTICATION", "openMethod": "openLogin"},
                  "locators": [
                    {
                      "id": "usernameInput",
                      "element": "username",
                      "strategy": "name",
                      "value": "username",
                      "role": "input",
                      "sameOrigin": true,
                      "uniqueWithinComponent": true,
                      "globalCount": 1,
                      "scopedCount": 1,
                      "score": 0.88,
                      "risks": []
                    }
                  ],
                  "actions": [],
                  "assertions": [],
                  "coverageGaps": [],
                  "rejectedSuggestions": []
                }
                """);

        Assert.assertEquals(contract.locators().size(), 1);
        Assert.assertEquals(contract.locators().get(0).elementName(), "username");
        Assert.assertEquals(contract.locators().get(0).stabilityScore(), 0.88d);
    }

    @Test
    public void flattensComponentContainerLocatorsReturnedByLlm() {
        PomContractSpec contract = new PomContractSpecParser().parse("""
                {
                  "schemaVersion": "pom-contract-v1",
                  "page": {"name": "DashboardPage", "route": "/dashboard/index", "capability": "DASHBOARD", "openMethod": "openDashboard"},
                  "locators": [
                    {
                      "id": "ContentComponent",
                      "type": "CONTENT",
                      "root": true,
                      "elements": [
                        {"id": "logoutLink", "element": "logoutControl", "strategy": "css", "value": "a[href*='logout']", "role": "button", "score": 0.79},
                        {"id": "pageTitle", "element": "pageTitle", "strategy": "css", "value": "h1, h2", "role": "page-title", "score": 0.76}
                      ]
                    }
                  ],
                  "components": [
                    {"name": "ContentComponent", "rootLocatorId": "ContentComponent", "ownedAssertions": ["URL_CONTAINS"]}
                  ],
                  "actions": [],
                  "assertions": [
                    {
                      "methodName": "isDashboardRouteVisible",
                      "returnType": "boolean",
                      "checks": [{"check": "URL_CONTAINS", "locator": "", "expectedValue": "/dashboard/index", "valueFrom": "", "attribute": "", "route": "/dashboard/index"}],
                      "combine": "AND"
                    }
                  ],
                  "coverageGaps": [],
                  "rejectedSuggestions": []
                }
                """);

        Assert.assertEquals(contract.locators().size(), 2);
        Assert.assertEquals(contract.locators().get(0).id(), "logoutLink");
        Assert.assertEquals(contract.locators().get(0).elementName(), "logoutControl");
        Assert.assertEquals(contract.locators().get(0).stabilityScore(), 0.79d);
        Assert.assertTrue(contract.components().isEmpty());
    }

    @Test
    public void stripsEvidenceMetadataFromAssertionsAndChecks() {
        PomContractSpec contract = new PomContractSpecParser().parse("""
                {
                  "schemaVersion": "pom-contract-v1",
                  "page": {"name": "DashboardPage", "route": "/dashboard/index", "capability": "AUTHENTICATED_AREA", "openMethod": "openDashboard"},
                  "locators": [
                    {"id": "dashboardHeader", "elementName": "Dashboard", "strategy": "css", "value": "h6", "role": "heading", "stabilityScore": 0.75}
                  ],
                  "actions": [],
                  "assertions": [
                    {
                      "methodName": "isDashboardVisible",
                      "returnType": "boolean",
                      "confidence": 0.91,
                      "requirementId": "REQ-017",
                      "ownerPage": "DashboardPage",
                      "notes": "LLM explanation must not be part of the strict contract",
                      "checks": [
                        {
                          "check": "VISIBLE",
                          "locator": "dashboardHeader",
                          "expectedValue": "",
                          "valueFrom": "",
                          "attribute": "",
                          "route": "",
                          "confidence": 0.91,
                          "notes": "Check-level explanation must be stripped too",
                          "sourceReference": "requirements/valid-login-requirement.md [L28]"
                        }
                      ],
                      "combine": "AND"
                    }
                  ],
                  "coverageGaps": [],
                  "rejectedSuggestions": []
                }
                """);

        Assert.assertEquals(contract.assertions().size(), 1);
        Assert.assertEquals(contract.assertions().get(0).methodName(), "isDashboardVisible");
        Assert.assertEquals(contract.assertions().get(0).checks().get(0).check(), PomCheckType.VISIBLE);
    }

    @Test
    public void removesLoginPageOutOfScopeCoverageNoise() {
        PomContractSpec contract = new PomContractSpecParser().parse("""
                {
                  "schemaVersion": "pom-contract-v1",
                  "page": {"name": "LoginPage", "route": "/auth/login", "capability": "AUTHENTICATION", "openMethod": "openLogin"},
                  "locators": [
                    {"id": "usernameInput", "elementName": "username", "strategy": "name", "value": "username", "role": "input", "stabilityScore": 0.87},
                    {"id": "passwordInput", "elementName": "password", "strategy": "name", "value": "password", "role": "password", "stabilityScore": 0.87},
                    {"id": "loginButton", "elementName": "login", "strategy": "css", "value": "button[type='submit']", "role": "button", "stabilityScore": 0.87}
                  ],
                  "actions": [],
                  "assertions": [
                    {
                      "methodName": "isLoginFormVisible",
                      "returnType": "boolean",
                      "checks": [
                        {"check": "VISIBLE", "locator": "usernameInput", "expectedValue": "", "valueFrom": "", "attribute": "", "route": ""},
                        {"check": "VISIBLE", "locator": "passwordInput", "expectedValue": "", "valueFrom": "", "attribute": "", "route": ""}
                      ],
                      "combine": "AND"
                    }
                  ],
                  "coverageGaps": [
                    "No explicit logout locator evidence because logout belongs to authenticated area.",
                    "FORM_VISIBLE unresolved as a single form locator but login field checks are available.",
                    "REQ-016 and REQ-019 lack explicit expected values.",
                    "Keep this real login page gap"
                  ],
                  "rejectedSuggestions": []
                }
                """);

        Assert.assertEquals(contract.coverageGaps(), java.util.List.of("Keep this real login page gap"));
    }
}
