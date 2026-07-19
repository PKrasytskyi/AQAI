package ua.demo.agentlab.ai.ui.prompt;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.runtime.skill.RuntimeSkillPromptLoader;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyAssertion;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyLocator;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;

import java.util.List;

public class AiPageObjectPromptBuilder {

    private final AiPromptContextFormatter formatter = new AiPromptContextFormatter();
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
            AiPageObjectSpec baselineSpec,
            PromptReadyPomScope promptScope,
            String confirmedCapability
    ) {
        String examplePageName = pageName == null || pageName.isBlank() ? "RequestedPage" : pageName;
        String exampleRoute = baselineSpec == null || baselineSpec.route().isBlank() ? "/" : baselineSpec.route();
        String exampleOpenMethod = baselineSpec == null || baselineSpec.openMethodName().isBlank()
                ? "open" + examplePageName.replaceAll("[^A-Za-z0-9]", "")
                : baselineSpec.openMethodName();
        return buildCompactPrompt(
                context,
                baselineSpec,
                examplePageName,
                exampleRoute,
                exampleOpenMethod,
                confirmedCapability,
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
        return "ownedActions=" + actions + " (exact public method signatures)"
                + System.lineSeparator() + "ownedAssertions=" + assertions;
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
        String target = assertion.targetLocatorId().isBlank()
                ? ""
                : "targetLocator=" + assertion.targetLocatorId() + ", ";
        return assertion.type() + "(" + target + "expected=" + assertion.expectedValue() + ")";
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
