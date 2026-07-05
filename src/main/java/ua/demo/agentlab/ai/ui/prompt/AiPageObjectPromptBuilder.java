package ua.demo.agentlab.ai.ui.prompt;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ui.UiTestScenario;

import java.util.List;

public class AiPageObjectPromptBuilder {

    private final AiPromptContextFormatter formatter = new AiPromptContextFormatter();
    private final PageObjectCapabilityContractFormatter capabilityContractFormatter =
            new PageObjectCapabilityContractFormatter();
    private final PageObjectPromptMode promptMode;

    public AiPageObjectPromptBuilder() {
        this(PageObjectPromptMode.fromRuntime());
    }

    public AiPageObjectPromptBuilder(PageObjectPromptMode promptMode) {
        this.promptMode = promptMode == null ? PageObjectPromptMode.COMPACT : promptMode;
    }

    public PageObjectPromptMode promptMode() {
        return promptMode;
    }

    public String buildForPage(
            AiContextPackage context,
            String pageName,
            List<UiTestScenario> pageScenarios,
            AiPageObjectSpec baselineSpec
    ) {
        String examplePageName = pageName == null || pageName.isBlank() ? "RequestedPage" : pageName;
        String exampleRoute = baselineSpec == null || baselineSpec.route().isBlank() ? "/" : baselineSpec.route();
        String exampleOpenMethod = baselineSpec == null || baselineSpec.openMethodName().isBlank()
                ? "open" + examplePageName.replaceAll("[^A-Za-z0-9]", "")
                : baselineSpec.openMethodName();
        if (promptMode == PageObjectPromptMode.DEBUG) {
            return buildDebugPrompt(context, pageName, baselineSpec, examplePageName, exampleRoute, exampleOpenMethod);
        }
        return buildCompactPrompt(
                context,
                pageName,
                pageScenarios,
                baselineSpec,
                examplePageName,
                exampleRoute,
                exampleOpenMethod,
                capabilityContractFormatter.capabilityFor(context, pageName)
        );
    }

    private String buildCompactPrompt(
            AiContextPackage context,
            String pageName,
            List<UiTestScenario> pageScenarios,
            AiPageObjectSpec baselineSpec,
            String examplePageName,
            String exampleRoute,
            String exampleOpenMethod,
            String exampleCapability
    ) {
        return """
                # Role
                You are a Page Object Contract Planner.
                Return only pom-contract-v1 JSON.
                You do not write Java.

                # Task
                Build one POM contract for:
                targetPage=%s
                targetRoute=%s
                capability=%s

                # Context
                %s

                # Authority
                1. Page capability contract.
                2. Page-owned actions/assertions.
                3. Allowed locators.
                4. Baseline API signatures only for naming compatibility.
                If evidence conflicts, prefer higher priority evidence.

                # Contract Rules
                - Generate only page-owned actions and page-owned assertions.
                - Do not create methods from baseline API unless also page-owned.
                - Do not create actions/assertions for prerequisite or target-after-navigation pages.
                - Use only allowed locators declared in this prompt.
                - The locators array must contain only flat locator objects: id, elementName, strategy, value, role, stabilityScore.
                - Do not put component containers, root objects, nested elements, or nested locators inside the locators array.
                - Do not expose locators to tests.
                - Do not create test logic or multi-page business flows.
                - Keep components empty in this compact contract; reusable component Java is handled by the deterministic writer later.
                - Add coverageGaps instead of inventing unsupported actions, assertions, routes, expected values, or locators.
                - Add coverageGaps when required locator evidence is missing, expected value is unresolved, target route is unclear, assertion cannot be backed by mapper evidence, or required action belongs to another page.
                - coverageGaps must be an array of plain strings.
                - For CLEAR_AND_TYPE, SEND_KEYS, SELECT_BY_VISIBLE_TEXT, and UPLOAD_FILE, valueFrom must equal a method parameter name or literalValue must be non-empty.

                # Vocabulary
                steps: CLICK, CLEAR_AND_TYPE, SEND_KEYS, SELECT_BY_VISIBLE_TEXT, UPLOAD_FILE, OPEN_ROUTE
                checks: VISIBLE, TEXT_CONTAINS, TEXT_EQUALS, TEXT_PRESENT, URL_CONTAINS, URL_EQUALS, ATTRIBUTE_EQUALS, COUNT_GREATER_THAN, LIST_TEXTS

                # Input
                Page capability contract:
                %s

                Scoped test cases:
                %s

                Required POM contract:
                %s

                Allowed locators:
                %s

                Baseline API signatures (naming hints only):
                %s

                # Output Schema
                Return JSON only in this shape. Schema version: %s.
                {
                  "schemaVersion": "pom-contract-v1",
                  "page": {"name": "%s", "route": "%s", "capability": "%s", "openMethod": "%s"},
                  "locators": [],
                  "components": [],
                  "actions": [
                    {
                      "methodName": "enterUsername",
                      "kind": "ACTION",
                      "parameters": [{"type": "String", "name": "username"}],
                      "steps": [{"action": "CLEAR_AND_TYPE", "locator": "usernameInput", "valueFrom": "username", "literalValue": "", "route": ""}]
                    }
                  ],
                  "assertions": [
                    {
                      "methodName": "<publicPageAssertion>",
                      "returnType": "boolean",
                      "checks": [{"check": "VISIBLE", "locator": "<declaredLocatorId>", "expectedValue": "", "valueFrom": "", "attribute": "", "route": ""}],
                      "combine": "AND"
                    }
                  ],
                  "coverageGaps": [],
                  "rejectedSuggestions": []
                }
                """.formatted(
                examplePageName,
                exampleRoute,
                exampleCapability == null || exampleCapability.isBlank() ? "UNKNOWN" : exampleCapability,
                formatter.summarizeCompactContext(context),
                capabilityContractFormatter.format(context, pageName, pageScenarios, baselineSpec),
                formatter.summarizeScopedPomTestCases(context, pageName, pageScenarios),
                formatter.summarizePromptRequiredContract(context, pageName, pageScenarios),
                formatter.summarizeAllowedPromptLocators(context),
                summarizeBaselineSpec(baselineSpec),
                LlmOutputSchemaVersion.POM_CONTRACT,
                examplePageName,
                exampleRoute,
                exampleCapability == null || exampleCapability.isBlank() ? "UNKNOWN" : exampleCapability,
                exampleOpenMethod
        );
    }

    private String buildDebugPrompt(
            AiContextPackage context,
            String pageName,
            AiPageObjectSpec baselineSpec,
            String examplePageName,
            String exampleRoute,
            String exampleOpenMethod
    ) {
        return """
                # Role
                You are a Page Object Contract Planner.

                You do not write Java.
                You do not write Selenium code.
                You only convert scoped UI knowledge into a deterministic Page Object contract.

                # Goal
                Generate exactly one deterministic POM contract for the scoped Selenium page capability confirmed by mapper evidence: %s.

                # Context
                %s

                # Input Authority Order
                Use evidence in this priority:
                1. Confirmed page capability contract.
                2. Assertion contracts.
                3. Mapper-approved locator candidates.
                4. PageModel enrichment facts.
                5. Current-run Neo4j/Qdrant evidence.
                6. Baseline POM spec.

                If evidence conflicts, prefer higher priority evidence.
                If required evidence is missing, add a coverageGap instead of inventing it.

                # Constraints
                1. Return JSON only; do not use markdown fences.
                2. Build exactly one POM contract for the requested page.
                3. Do not write Java method bodies.
                4. Do not use Selenium, WebDriver, WebElement, By, waits, or helper method calls in output.
                5. Use only structured steps and checks from the schema.
                5a. Allowed step actions are exactly: CLICK, CLEAR_AND_TYPE, SEND_KEYS, SELECT_BY_VISIBLE_TEXT, UPLOAD_FILE, OPEN_ROUTE.
                5b. Do not use aliases such as TYPE, OPEN_ROUTE_CHECK, COVERAGE_GAP, FORM_VISIBLE, ELEMENT_VISIBLE, or ROUTE_CONTAINS in JSON fields.
                5c. For typed input steps, use CLEAR_AND_TYPE and set valueFrom to the exact method parameter name, for example username or password.
                5d. Allowed check types are exactly: VISIBLE, TEXT_CONTAINS, TEXT_EQUALS, TEXT_PRESENT, URL_CONTAINS, URL_EQUALS, ATTRIBUTE_EQUALS, COUNT_GREATER_THAN, LIST_TEXTS.
                6. ACTION methods may click, type, select, upload, or navigate inside the app.
                7. ASSERTION methods may return boolean, String, or List<String>.
                8. Do not mix action and assertion in one method.
                9. Do not create test logic.
                10. Do not create business flows spanning multiple pages.
                11. Do not expose locators to tests.
                12. Use only mapper-approved locators or baseline locators that do not conflict with mapper evidence.
                13. Do not add locators or navigation methods for external origins.
                14. Add coverageGaps instead of inventing unsupported actions, assertions, routes, expected values, or locators.
                15. Baseline page object spec is compatibility context only; do not create baseline methods unless they are also listed in Page-owned actions or Page-owned assertions.
                16. coverageGaps must be an array of plain strings, not objects.

                # Input
                Page capability contract:
                %s

                Prompt UI evidence:
                %s

                PageModel enrichment facts:
                %s

                Relevant knowledge retrieved from Neo4j/Qdrant:
                %s

                Baseline page object spec:
                %s

                # Expected Output
                Return JSON in this exact shape. Schema version: %s.
                This is a neutral contract example only:
                - Keep page.name, page.route, and page.openMethod aligned with the requested page and baseline spec.
                - Use only locator ids that are declared in locators.
                - Do not copy domain-specific actions unless this page's scoped evidence requires them.
                - Use coverageGaps for missing required evidence.
                {
                  "schemaVersion": "pom-contract-v1",
                  "page": {
                    "name": "%s",
                    "route": "%s",
                    "capability": "<confirmed page capability>",
                    "openMethod": "%s"
                  },
                  "locators": [
                    {
                      "id": "<camelCaseLocatorId>",
                      "elementName": "<semantic element name>",
                      "strategy": "<id|name|css|xpath|partialLinkText>",
                      "value": "<stable locator value from mapper evidence or baseline>",
                      "role": "<input|button|link|message|container|unknown>",
                      "stabilityScore": 0.90
                    }
                  ],
                  "actions": [
                    {
                      "methodName": "<publicPageAction>",
                      "kind": "ACTION",
                      "parameters": [{"type": "<JavaType>", "name": "<parameterName>"}],
                      "steps": [
                        {"action": "CLICK", "locator": "<declaredLocatorId>", "valueFrom": "", "literalValue": "", "route": ""}
                      ]
                    }
                  ],
                  "assertions": [
                    {
                      "methodName": "<publicPageAssertion>",
                      "returnType": "boolean",
                      "checks": [
                        {"check": "VISIBLE", "locator": "<declaredLocatorId>", "expectedValue": "", "valueFrom": "", "attribute": "", "route": ""}
                      ],
                      "combine": "AND"
                    }
                  ],
                  "coverageGaps": [],
                  "rejectedSuggestions": []
                }

                # Success Criteria
                - The contract exposes a stable reusable public API for all scoped test cases on this page.
                - Every required action or assertion that cannot use inherited BasePage methods has a dedicated contract method.
                - Boolean state methods use meaningful page-specific evidence: route fragments, visible elements, expected attributes, or expected text.
                - No weak URL nonblank assertions are represented.
                - Every step and check references declared locator ids unless it is a route check or OPEN_ROUTE step.
                - coverageGaps clearly explain missing evidence.

                # Coverage Gap Rules
                Add coverageGap when:
                - required locator is missing;
                - expected value is unresolved;
                - target route is unclear;
                - assertion cannot be backed by mapper evidence;
                - required action belongs to another page.
                """.formatted(
                examplePageName,
                formatter.summarize(context, pageName),
                capabilityContractFormatter.format(context, pageName, baselineSpec),
                formatter.summarizePromptUiEvidence(context),
                formatter.summarizePageModelEnrichments(context, pageName),
                formatter.summarizeStructuredRetrievalContext(context),
                summarizeBaselineSpec(baselineSpec),
                LlmOutputSchemaVersion.POM_CONTRACT,
                examplePageName,
                exampleRoute,
                exampleOpenMethod
        );
    }

    private String summarizeBaselineSpec(AiPageObjectSpec baselineSpec) {
        if (baselineSpec == null) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("- pageName=").append(baselineSpec.pageName())
                .append(" | route=").append(baselineSpec.route())
                .append(" | openMethodName=").append(baselineSpec.openMethodName())
                .append(System.lineSeparator());
        builder.append("- methodSignatures=").append(baselineSpec.methods().stream()
                .map(method -> method.returnType()
                        + " "
                        + method.methodName()
                        + "("
                        + method.parameters().stream()
                        .map(parameter -> parameter.type() + " " + parameter.name())
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("")
                        + ")")
                .toList());
        return builder.toString();
    }
}
