package ua.demo.agentlab.ai.ui.prompt;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;
import ua.demo.agentlab.ui.UiTestScenario;

import java.util.List;

public class AiUiTestPromptBuilder {

    private final AiUiTestPromptContextFormatter formatter = new AiUiTestPromptContextFormatter();

    public String buildForScenario(
            AiContextPackage context,
            UiTestScenario scenario,
            List<AiPageObjectSpec> pageObjectSpecs,
            AiUiTestSpec baselineSpec
    ) {
        return """
                # Goal
                Generate exactly one atomic Selenium + TestNG UI test spec for the supplied defined test case.

                # Context
                %s

                # Constraints
                %s

                # Input
                Defined test case:
                %s

                Public page object API available to this scenario:
                %s

                Baseline test spec:
                %s

                # Expected Output
                Return JSON in this exact shape. Schema version: %s.
                - Replace placeholder values with concrete names, bodies, imports, and scenario data from the supplied input.
                {
                  "schemaVersion": "%s",
                  "tests": [
                    {
                      "scenarioId": "<defined scenario id>",
                      "className": "<TestClassName>",
                      "sourcePageClassName": "<SourcePageClassName>",
                      "sourcePageVariableName": "<sourcePageVariable>",
                      "pageClassName": "<PrimaryAssertPageClassName>",
                      "pageVariableName": "<primaryAssertPageVariable>",
                      "testMethodName": "<atomicTestMethodName>",
                      "testDescription": "<business requirement summary>",
                      "actionBody": "<valid Java statements using only supplied page-object API and BaseTest helpers>",
                      "assertionBody": "<valid Java assertions using UiAssertions and supplied page-object API>",
                      "additionalImports": []
                    }
                  ]
                }

                # Success Criteria
                - The test covers only the supplied defined test case.
                - The test creates all required UI state inside the same test method.
                - The test uses only supplied page-object public methods, BaseTest helpers, UiAssertions, and inherited BasePage methods.
                - Assertions validate the business requirement with concrete page evidence such as expected route fragments, visible page-specific content, or expected text.
                - The test does not call Selenium, locators, WebElements, driver, waits, or page internals.
                - The generated actionBody and assertionBody are compile-ready Java statements.

                # Notes
                If the supplied public page-object API cannot express the required behavior, do not invent new page methods in the test.
                """.formatted(
                formatter.summarizeContext(context),
                formatter.summarizePolicyRules(context),
                formatter.summarizeDefinedTestCase(context, scenario),
                formatter.summarizePublicPageObjectApi(pageObjectSpecs),
                baselineSpec == null ? "none" : baselineSpec.toString(),
                LlmOutputSchemaVersion.AI_UI_TEST_SPEC,
                LlmOutputSchemaVersion.AI_UI_TEST_SPEC
        );
    }
}
