package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.prompt.AiPageObjectPromptBuilder;
import ua.demo.agentlab.ai.ui.prompt.scope.PomScopeSanitizer;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ui.UiTestScenario;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiPageObjectPromptBuildStage {

    private final AiPageObjectPromptBuilder promptBuilder;
    private final PomScopeSanitizer scopeSanitizer;

    public AiPageObjectPromptBuildStage() {
        this(new AiPageObjectPromptBuilder(), new PomScopeSanitizer());
    }

    AiPageObjectPromptBuildStage(AiPageObjectPromptBuilder promptBuilder) {
        this(promptBuilder, new PomScopeSanitizer());
    }

    AiPageObjectPromptBuildStage(AiPageObjectPromptBuilder promptBuilder, PomScopeSanitizer scopeSanitizer) {
        if (promptBuilder == null) {
            throw new IllegalArgumentException("promptBuilder cannot be null");
        }
        this.promptBuilder = promptBuilder;
        this.scopeSanitizer = scopeSanitizer == null ? new PomScopeSanitizer() : scopeSanitizer;
    }

    public AiPageObjectPromptDraft build(AiPageObjectPromptScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("scope cannot be null");
        }
        String prompt = promptBuilder.buildForPage(
                scope.scopedContext(),
                scope.pageName(),
                scope.pageScenarios(),
                scope.baselineSpec()
        );
        return new AiPageObjectPromptDraft(scope, prompt, buildPromptMetadata(scope));
    }

    private Map<String, Object> buildPromptMetadata(AiPageObjectPromptScope scope) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        PromptReadyPomScope promptScope = scopeSanitizer.sanitize(
                scope.scopedContext(), scope.pageName(), scope.pageScenarios());
        List<UiTestScenario> pageScenarios = scope.pageScenarios();
        metadata.put("schemaVersion", LlmOutputSchemaVersion.POM_CONTRACT);
        metadata.put("compatibilityOutput", LlmOutputSchemaVersion.AI_PAGE_OBJECT_SPEC);
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
}
