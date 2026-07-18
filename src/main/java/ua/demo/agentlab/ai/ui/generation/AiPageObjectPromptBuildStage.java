package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.prompt.AiPageObjectPromptBuilder;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ai.ui.prompt.scope.ConfirmedUiCatalogPomScopeProjector;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiPageObjectPromptBuildStage {

    private final AiPageObjectPromptBuilder promptBuilder;
    private final ConfirmedUiCatalogPomScopeProjector catalogProjector = new ConfirmedUiCatalogPomScopeProjector();

    public AiPageObjectPromptBuildStage() {
        this(new AiPageObjectPromptBuilder());
    }

    AiPageObjectPromptBuildStage(AiPageObjectPromptBuilder promptBuilder) {
        if (promptBuilder == null) {
            throw new IllegalArgumentException("promptBuilder cannot be null");
        }
        this.promptBuilder = promptBuilder;
    }

    public AiPageObjectPromptDraft build(AiPageObjectPromptScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("scope cannot be null");
        }
        PromptReadyPomScope promptScope = readyScope(scope);
        String prompt = promptBuilder.buildForPage(
                scope.scopedContext(), scope.pageName(), scope.baselineSpec(), promptScope, capability(scope));
        return new AiPageObjectPromptDraft(scope, prompt, buildPromptMetadata(scope));
    }

    private Map<String, Object> buildPromptMetadata(AiPageObjectPromptScope scope) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        PromptReadyPomScope promptScope = readyScope(scope);
        List<UiTestScenario> pageScenarios = scope.pageScenarios();
        metadata.put("schemaVersion", LlmOutputSchemaVersion.POM_CONTRACT);
        metadata.put("javaRenderingModel", LlmOutputSchemaVersion.AI_PAGE_OBJECT_SPEC);
        metadata.put("promptMode", promptBuilder.promptMode().name().toLowerCase(java.util.Locale.ROOT));
        metadata.put("pageName", scope.pageName());
        metadata.put("scopedScenarioCount", pageScenarios.size());
        metadata.put("scopedScenarioIds", pageScenarios.stream().map(UiTestScenario::id).toList());
        metadata.put("hasBaselineSpec", scope.baselineSpec() != null);
        metadata.put("mappedPageCount", scope.scopedContext() == null || scope.scopedContext().mappedUiKnowledge() == null
                ? 0
                : scope.scopedContext().mappedUiKnowledge().pages().size());
        // These counts describe the final sanitized scope actually given to the model.
        // Raw PromptUiEvidence is deliberately broader and must never inflate run quality.
        metadata.put("promptAllowedLocatorCount", promptScope.allowedLocators().size());
        metadata.put("promptActionCount", promptScope.ownedActions().size());
        metadata.put("promptAssertionCount", promptScope.ownedAssertions().size());
        metadata.put("pageModelPageCount", scope.scopedContext() == null || scope.scopedContext().pageModelBundle() == null
                ? 0
                : scope.scopedContext().pageModelBundle().pages().size());
        metadata.put("canonicalCaseCount", scope.scopedContext() == null || scope.scopedContext().canonicalTestCaseBundle() == null
                ? 0
                : scope.scopedContext().canonicalTestCaseBundle().testCases().size());
        return metadata;
    }

    private PromptReadyPomScope readyScope(AiPageObjectPromptScope scope) {
        return catalogProjector.project(scope.confirmedUiCatalog(), scope.pageName());
    }

    private String capability(AiPageObjectPromptScope scope) {
        if (scope.confirmedUiCatalog() == null) return "UNKNOWN";
        return scope.confirmedUiCatalog().pages().stream()
                .filter(page -> PageReferenceMatcher.matchesScenarioPage(
                        page.pageName(), page.route(), scope.pageName()))
                .map(page -> page.capability())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("UNKNOWN");
    }
}
