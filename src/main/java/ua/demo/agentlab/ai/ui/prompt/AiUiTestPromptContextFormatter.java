package ua.demo.agentlab.ai.ui.prompt;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodSpec;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.UiTestScenario;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AiUiTestPromptContextFormatter {

    public String summarizeContext(AiContextPackage context) {
        if (context == null) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Objective: ").append(context.objective()).append(System.lineSeparator());
        if (context.projectProfile() != null) {
            builder.append("Project: ").append(context.projectProfile().projectName())
                    .append(" | baseUrl=").append(context.projectProfile().baseUrl())
                    .append(System.lineSeparator());
            builder.append("Routes: ").append(summarizeConfiguredRoutes(context.projectProfile()))
                    .append(System.lineSeparator());
        }
        builder.append("Policy: ").append(summarizePolicy(context.generationPolicy()));
        return builder.toString().strip();
    }

    public String summarizePolicyRules(AiContextPackage context) {
        GenerationPolicy policy = context == null ? null : context.generationPolicy();
        String policyId = policy == null ? "unknown" : policy.policyId();
        boolean reviewGate = policy != null && policy.requireReviewGate();
        boolean compileGate = policy != null && policy.requireCompileGate();
        return """
                1. Return JSON only, matching the requested schema exactly.
                2. Generate exactly one atomic TestNG test for the supplied defined test case.
                3. Use only the supplied public page-object API plus BaseTest helpers, UiAssertions, and inherited BasePage methods.
                4. Do not access Selenium, locators, WebElements, driver, waits, or page internals from tests.
                5. Create all required UI state inside the same test method; never rely on another test or execution order.
                6. Use credentials(profileName) or scenarioData(dataSetName) only when the defined test case needs them.
                7. Do not invent page methods outside the supplied public page-object API.
                8. Do not add fallback branches, comments, assumptions, simulated steps, or hidden missing coverage.
                9. Follow policy %s; reviewGate=%s, compileGate=%s.
                """.formatted(policyId, reviewGate, compileGate).strip();
    }

    public String summarizeDefinedTestCase(AiContextPackage context, UiTestScenario scenario) {
        if (scenario == null) {
            return "- none";
        }
        CanonicalTestCase canonical = findCanonicalTestCase(context, scenario.id());
        String title = canonical == null ? scenario.title() : canonical.title();
        List<String> actions = canonical == null ? scenario.actions() : canonical.actions();
        List<String> assertions = canonical == null ? scenario.assertions() : canonical.assertions();
        List<?> operations = canonical == null ? scenario.operationIntents() : canonical.operationIntents();
        List<?> assertionIntents = canonical == null ? scenario.assertionIntents() : canonical.assertionIntents();
        String sourceReference = canonical == null ? scenario.sourceReference() : canonical.sourceReference();

        StringBuilder builder = new StringBuilder();
        builder.append("Scenario: ").append(scenario.id()).append(" | ").append(title).append(System.lineSeparator());
        builder.append("Business requirement: ").append(title).append(System.lineSeparator());
        builder.append("Source page: ").append(scenario.sourcePageName())
                .append(" | target page: ").append(scenario.pageName())
                .append(" | target route: ").append(scenario.route())
                .append(System.lineSeparator());
        builder.append("Source reference: ").append(sourceReference).append(System.lineSeparator());
        builder.append("Operations: ").append(operations).append(System.lineSeparator());
        builder.append("Actions: ").append(actions).append(System.lineSeparator());
        builder.append("Assertions: ").append(assertions).append(System.lineSeparator());
        builder.append("Assertion intents: ").append(assertionIntents).append(System.lineSeparator());
        builder.append("Assertion contracts: ").append(assertionContracts(context, scenario.id()));
        return builder.toString();
    }

    public String summarizePublicPageObjectApi(List<AiPageObjectSpec> pageObjectSpecs) {
        if (pageObjectSpecs == null || pageObjectSpecs.isEmpty()) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        for (AiPageObjectSpec spec : pageObjectSpecs) {
            builder.append("- ").append(spec.pageName())
                    .append(" | route=").append(spec.route())
                    .append(" | openMethod=").append(spec.openMethodName())
                    .append(System.lineSeparator());
            spec.methods().stream()
                    .filter(method -> !sameMethod(method.methodName(), spec.openMethodName()))
                    .forEach(method -> builder.append("  method: ")
                            .append(signature(method))
                            .append(System.lineSeparator()));
            builder.append("  inherited: getCurrentUrl(), getTitle(), getPageSource(), open(relativePath), openAbsolute(url)")
                    .append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    private CanonicalTestCase findCanonicalTestCase(AiContextPackage context, String scenarioId) {
        if (context == null || context.canonicalTestCaseBundle() == null || scenarioId == null) {
            return null;
        }
        return context.canonicalTestCaseBundle().testCases().stream()
                .filter(testCase -> scenarioId.equals(testCase.id()))
                .findFirst()
                .orElse(null);
    }

    private List<AssertionContract> assertionContracts(AiContextPackage context, String scenarioId) {
        if (context == null || context.assertionContracts().isEmpty() || scenarioId == null) {
            return List.of();
        }
        return context.assertionContracts().stream()
                .filter(contract -> scenarioId.equals(contract.testCaseId()))
                .toList();
    }

    private boolean sameMethod(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private String signature(AiMethodSpec method) {
        String parameters = method.parameters().stream()
                .map(this::parameterSignature)
                .collect(Collectors.joining(", "));
        return "%s %s(%s)".formatted(method.returnType(), method.methodName(), parameters);
    }

    private String parameterSignature(AiMethodParameterSpec parameter) {
        return "%s %s".formatted(parameter.type(), parameter.name()).trim();
    }

    private String summarizePolicy(GenerationPolicy policy) {
        if (policy == null) {
            return "none";
        }
        return "%s, aiSuggestions=%s, reviewGate=%s, compileGate=%s".formatted(
                policy.policyId(),
                policy.allowAiSuggestions(),
                policy.requireReviewGate(),
                policy.requireCompileGate()
        );
    }

    private String summarizeConfiguredRoutes(ua.demo.agentlab.config.ProjectProfile profile) {
        List<String> routes = new java.util.ArrayList<>();
        addRoute(routes, "home", profile.homeRoute());
        addRoute(routes, "login", profile.loginRoute());
        addRoute(routes, "authenticated", profile.authenticatedRoute());
        addRoute(routes, "security", profile.securityRoute());
        addRoute(routes, "details", profile.detailsRoute());
        addRoute(routes, "form", profile.formRoute());
        addRoute(routes, "catalog", profile.catalogRoute());
        addRoute(routes, "products", profile.productsRoute());
        addRoute(routes, "cart", profile.cartRoute());
        return routes.isEmpty() ? "none configured" : String.join(", ", routes);
    }

    private void addRoute(List<String> routes, String name, String route) {
        if (route != null && !route.isBlank()) {
            routes.add(name + "=" + route);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
