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
        return """
                # Goal
                Generate exactly one Page Object spec for the scoped Selenium page capability confirmed by mapper evidence: %s.

                # Context
                %s

                # Constraints
                1. Return JSON only; do not use markdown fences.
                2. Build exactly one page object spec for the requested page.
                3. Use only the project's BasePage API in method bodies.
                4. Available inherited public BasePage methods: getCurrentUrl(), getTitle(), getPageSource(), open(relativePath), openAbsolute(url).
                5. Available page-object internals: elements.isVisible(By), elements.click(By), elements.findAll(By), elements.sendKeys(By, String), elements.clearAndType(By, String), elements.text(By), elements.attribute(By, String).
                6. Never use elements.type(...); it is not part of the BasePage API.
                7. If a locator is declared in locators, method bodies must reference that private locator field instead of repeating inline By selectors.
                8. Do not expose Selenium internals to tests: driver, waits, locators, page.elements, or raw WebElements are page-object implementation details.
                9. Implement actions only on the page where the action is performed; result pages should expose state/assertion methods.
                10. Reuse the baseline page API when it already covers the required behavior; extend it only for the supplied defined test cases.
                11. Do not add locators or navigation methods for external origins; Page Objects operate only within the configured application base URL.

                # Input
                Page capability contract:
                %s

                PageModel enrichment facts:
                %s

                Relevant knowledge retrieved from Neo4j/Qdrant:
                %s

                Baseline page object spec:
                %s

                # Expected Output
                Return JSON in this exact shape. Schema version: %s.
                This is a neutral schema example only:
                - Keep pageName, route, and openMethodName aligned with the requested page and baseline spec.
                - Replace placeholder locator and method names with names supported by the defined test cases and enrichment facts.
                - Do not copy domain-specific methods unless this page's scoped test cases require them.
                {
                  "schemaVersion": "%s",
                  "pageObjects": [
                    {
                      "pageName": "%s",
                      "route": "%s",
                      "openMethodName": "%s",
                      "locators": [
                        {
                          "fieldName": "<camelCaseLocatorField>",
                          "elementName": "<semantic element name>",
                          "strategy": "<id|name|css|xpath|partialLinkText>",
                          "value": "<stable locator value from mapper evidence or baseline>"
                        }
                      ],
                      "methods": [
                        {
                          "returnType": "<void|boolean|String|List<String>>",
                          "methodName": "<publicPageActionOrAssertion>",
                          "parameters": [{"type": "<JavaType>", "name": "<parameterName>"}],
                          "body": "<valid Java statements using only declared locators and BasePage helpers>",
                          "requiredImports": []
                        }
                      ]
                    }
                  ]
                }

                # Success Criteria
                - The page object exposes a stable reusable public API for all scoped test cases on this page.
                - Every required action or assertion that cannot use inherited BasePage methods has a dedicated public page method.
                - Boolean state methods use meaningful page-specific evidence: route fragments, visible elements, expected attributes, or expected text.
                - No method returns weak checks such as !getCurrentUrl().isBlank().
                - Generated Java statements compile with declared locators and required imports.

                # Notes
                Treat entityKey as the display name from requirements or scenario data. Do not convert it to a route slug unless an explicit route value is provided.
                """.formatted(
                examplePageName,
                formatter.summarize(context, pageName),
                capabilityContractFormatter.format(context, pageName, baselineSpec),
                formatter.summarizePageModelEnrichments(context, pageName),
                formatter.summarizeStructuredRetrievalContext(context),
                baselineSpec == null ? "none" : baselineSpec.toString(),
                LlmOutputSchemaVersion.AI_PAGE_OBJECT_SPEC,
                LlmOutputSchemaVersion.AI_PAGE_OBJECT_SPEC,
                examplePageName,
                exampleRoute,
                exampleOpenMethod
        );
    }
}
