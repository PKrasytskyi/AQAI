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
        return buildCompactPrompt(context, pageName, pageScenarios, baselineSpec, examplePageName, exampleRoute, exampleOpenMethod);
    }

    private String buildCompactPrompt(
            AiContextPackage context,
            String pageName,
            List<UiTestScenario> pageScenarios,
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
                Generate exactly one deterministic POM contract for the scoped Selenium page capability: %s.

                # Context
                %s

                # Input Authority Order
                Use evidence in this priority:
                1. Confirmed page capability contract.
                2. Page-owned assertion contracts and expected values.
                3. Mapper-approved allowed locators.
                4. Curated enrichment signals already assembled into this prompt.
                5. Baseline POM API signatures.

                If evidence conflicts, prefer higher priority evidence.
                If required evidence is missing, add a coverageGap instead of inventing it.

                # Constraints
                1. Return JSON only; do not use markdown fences.
                2. Build exactly one POM contract for the requested page.
                3. Do not write Java method bodies.
                4. Do not use Selenium, WebDriver, WebElement, By, waits, or helper method calls in output.
                5. Use only structured steps and checks from the schema.
                6. ACTION methods may click, type, select, upload, or navigate inside the app.
                7. ASSERTION methods may return boolean, String, or List<String>.
                8. Do not mix action and assertion in one method.
                9. Do not create test logic.
                10. Do not create business flows spanning multiple pages.
                11. Do not expose locators to tests.
                12. Use only allowed locators declared in this prompt or safe baseline locators.
                13. Do not add locators or navigation methods for external origins.
                14. Add coverageGaps instead of inventing unsupported actions, assertions, routes, expected values, or locators.

                # Input
                Page capability contract:
                %s

                Required POM contract:
                %s

                Allowed locators:
                %s

                Baseline page object API:
                %s

                # Expected Output
                Return JSON in this exact shape. Schema version: %s.
                This is a neutral contract example only:
                - Keep page.name, page.route, and page.openMethod aligned with the requested page and baseline spec.
                - Use only locator ids that are declared in locators.
                - Put reusable or complex scoped UI regions in components: sidebar, header, search, table, modal, widget.
                - Keep simple one-off controls in page locators/actions/assertions.
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
                      "value": "<stable locator value from allowed locators or safe baseline>",
                      "role": "<input|button|link|message|container|unknown>",
                      "stabilityScore": 0.90
                    }
                  ],
                  "components": [
                    {
                      "name": "<SidebarComponent|HeaderComponent|SearchComponent|TableComponent|WidgetComponent>",
                      "type": "<NAVIGATION|HEADER|SEARCH|TABLE|MODAL|WIDGET|FORM>",
                      "rootLocatorId": "<declared page locator id or first component locator id>",
                      "locators": [
                        {
                          "id": "<componentScopedLocatorId>",
                          "elementName": "<semantic element name>",
                          "strategy": "<id|name|css|xpath|partialLinkText>",
                          "value": "<stable scoped locator value from allowed locators>",
                          "role": "<input|button|link|message|container|unknown>",
                          "stabilityScore": 0.90
                        }
                      ],
                      "actions": [],
                      "assertions": [],
                      "reusable": true
                    }
                  ],
                  "actions": [
                    {
                      "methodName": "<publicPageAction>",
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
                - The contract exposes a stable reusable public API for all scoped page-owned requirements.
                - Every required page-owned action or assertion that cannot use inherited BasePage methods has a dedicated contract method.
                - Boolean state methods use meaningful page-specific evidence: route fragments, visible elements, expected attributes, or expected text.
                - No weak URL nonblank assertions are represented.
                - Every step and check references declared locator ids unless it is a route check or OPEN_ROUTE step.
                - Reusable SPA regions are represented as components instead of page-owned business methods.
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
                formatter.summarizeCompactContext(context),
                capabilityContractFormatter.format(context, pageName, baselineSpec),
                formatter.summarizePromptRequiredContract(context, pageName, pageScenarios),
                formatter.summarizeAllowedPromptLocators(context),
                summarizeBaselineSpec(baselineSpec),
                LlmOutputSchemaVersion.POM_CONTRACT,
                examplePageName,
                exampleRoute,
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
                6. ACTION methods may click, type, select, upload, or navigate inside the app.
                7. ASSERTION methods may return boolean, String, or List<String>.
                8. Do not mix action and assertion in one method.
                9. Do not create test logic.
                10. Do not create business flows spanning multiple pages.
                11. Do not expose locators to tests.
                12. Use only mapper-approved locators or baseline locators that do not conflict with mapper evidence.
                13. Do not add locators or navigation methods for external origins.
                14. Add coverageGaps instead of inventing unsupported actions, assertions, routes, expected values, or locators.

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
        builder.append("- locators=").append(baselineSpec.locators().stream()
                .map(locator -> locator.fieldName()
                        + "(" + locator.strategy() + "=" + locator.value() + ")")
                .toList())
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
