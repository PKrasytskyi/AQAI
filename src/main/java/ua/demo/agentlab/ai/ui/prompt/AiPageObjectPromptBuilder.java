package ua.demo.agentlab.ai.ui.prompt;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.runtime.skill.RuntimeSkillPromptLoader;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.prompt.scope.PomScopeSanitizer;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyAssertion;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyLocator;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ui.UiTestScenario;

import java.util.List;

public class AiPageObjectPromptBuilder {

    private final AiPromptContextFormatter formatter = new AiPromptContextFormatter();
    private final PageObjectCapabilityContractFormatter capabilityContractFormatter =
            new PageObjectCapabilityContractFormatter();
    private final PomScopeSanitizer pomScopeSanitizer = new PomScopeSanitizer();
    private final PageObjectPromptMode promptMode;
    private final RuntimeSkillPromptLoader skillPromptLoader;

    public AiPageObjectPromptBuilder() {
        this(PageObjectPromptMode.fromRuntime());
    }

    public AiPageObjectPromptBuilder(PageObjectPromptMode promptMode) {
        this(promptMode, new RuntimeSkillPromptLoader());
    }

    AiPageObjectPromptBuilder(PageObjectPromptMode promptMode, RuntimeSkillPromptLoader skillPromptLoader) {
        this.promptMode = promptMode == null ? PageObjectPromptMode.COMPACT : promptMode;
        this.skillPromptLoader = skillPromptLoader == null ? new RuntimeSkillPromptLoader() : skillPromptLoader;
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
        PromptReadyPomScope promptScope = pomScopeSanitizer.sanitize(context, pageName, pageScenarios);
        return buildCompactPrompt(
                context,
                baselineSpec,
                examplePageName,
                exampleRoute,
                exampleOpenMethod,
                capabilityContractFormatter.capabilityFor(context, pageName),
                promptScope
        );
    }

    private String buildCompactPrompt(
            AiContextPackage context,
            AiPageObjectSpec baselineSpec,
            String examplePageName,
            String exampleRoute,
            String exampleOpenMethod,
            String exampleCapability,
            PromptReadyPomScope promptScope
    ) {
        return """
                # Runtime Skill Contract
                %s

                # Task
                Build one POM contract for:
                targetPage=%s
                targetRoute=%s
                capability=%s

                Contract schemaVersion=%s

                # Project Context
                %s

                # Input
                Typed page contract:
                %s

                Page-owned actions and assertions:
                %s

                Required coverage gaps:
                %s

                Confirmed selected locators:
                %s

                Baseline API signatures (naming hints only):
                %s
                """.formatted(
                skillPromptLoader.promptBlock("pom-json-generation"),
                examplePageName,
                exampleRoute,
                exampleCapability == null || exampleCapability.isBlank() ? "UNKNOWN" : exampleCapability,
                LlmOutputSchemaVersion.POM_CONTRACT,
                formatter.summarizeCompactContext(context),
                formatTypedPageContract(promptScope, examplePageName, exampleRoute, exampleOpenMethod, exampleCapability),
                formatOwnedContract(promptScope),
                formatCoverageGaps(promptScope),
                formatSelectedLocators(promptScope),
                summarizeBaselineSpec(baselineSpec, promptScope)
        );
    }

    private String formatTypedPageContract(
            PromptReadyPomScope scope,
            String pageName,
            String route,
            String openMethod,
            String capability
    ) {
        String scopePage = scope == null || scope.targetPage().isBlank() ? pageName : scope.targetPage();
        String scopeRoute = scope == null || scope.targetRoute().isBlank() ? route : scope.targetRoute();
        return "pageName=%s | route=%s | capability=%s | openMethodName=%s | requiresAuthentication=%s | prerequisitePages=%s"
                .formatted(scopePage, scopeRoute, capability == null || capability.isBlank() ? "UNKNOWN" : capability,
                        openMethod, scope != null && scope.requiresAuthentication(),
                        scope == null || scope.prerequisitePages().isEmpty() ? "none" : scope.prerequisitePages());
    }

    private String formatOwnedContract(PromptReadyPomScope scope) {
        if (scope == null) return "ownedActions=none\nownedAssertions=none";
        String actions = scope.ownedActions().isEmpty() ? "none" : String.join(", ", scope.ownedActions());
        String assertions = scope.ownedAssertions().isEmpty() ? "none" : scope.ownedAssertions().stream()
                .map(this::formatAssertion)
                .reduce((left, right) -> left + "; " + right)
                .orElse("none");
        return "ownedActions=" + actions + System.lineSeparator() + "ownedAssertions=" + assertions;
    }

    private String formatCoverageGaps(PromptReadyPomScope scope) {
        if (scope == null || scope.coverageGaps().isEmpty()) {
            return "none";
        }
        return scope.coverageGaps().stream()
                .map(gap -> "- " + gap + " Copy this exact gap into coverageGaps; do not create the unsupported action or assertion.")
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("none");
    }

    private String formatAssertion(PromptReadyAssertion assertion) {
        return assertion.type() + "(" + assertion.expectedValue() + ")";
    }

    private String formatSelectedLocators(PromptReadyPomScope scope) {
        if (scope == null || scope.allowedLocators().isEmpty()) return "none";
        return scope.allowedLocators().stream()
                .map(this::formatLocator)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("none");
    }

    private String formatLocator(PromptReadyLocator locator) {
        return "- %s | strategy=%s | value=%s | role=%s | component=%s | score=%.2f"
                .formatted(locator.id(), locator.strategy(), locator.value(), locator.role(),
                        locator.componentName().isBlank() ? "PageScope" : locator.componentName(), locator.score());
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
                # Runtime Skill Contract
                %s

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
                5. Retrieval health metadata already applied before prompt assembly.
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

                Retrieval health metadata:
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
                skillPromptLoader.promptBlock("pom-json-generation"),
                examplePageName,
                formatter.summarize(context, pageName),
                capabilityContractFormatter.format(context, pageName, baselineSpec),
                formatter.summarizePromptUiEvidence(context),
                formatter.summarizePageModelEnrichments(context, pageName),
                formatter.summarizeStructuredRetrievalContext(context),
                summarizeBaselineSpec(baselineSpec, null),
                LlmOutputSchemaVersion.POM_CONTRACT,
                examplePageName,
                exampleRoute,
                exampleOpenMethod
        );
    }

    private String summarizeBaselineSpec(AiPageObjectSpec baselineSpec, PromptReadyPomScope scope) {
        if (baselineSpec == null) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("- pageName=").append(baselineSpec.pageName())
                .append(" | route=").append(baselineSpec.route())
                .append(" | openMethodName=").append(baselineSpec.openMethodName())
                .append(System.lineSeparator());
        List<String> allowedMethods = scope == null ? List.of() : scope.ownedActions().stream()
                .map(method -> method.substring(0, method.indexOf('(') < 0 ? method.length() : method.indexOf('(')))
                .toList();
        builder.append("- methodSignatures=").append(baselineSpec.methods().stream()
                .filter(method -> allowedMethods.isEmpty() || allowedMethods.contains(method.methodName()))
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
