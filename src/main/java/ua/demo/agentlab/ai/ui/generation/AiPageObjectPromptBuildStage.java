package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.prompt.AiPageObjectPromptBuilder;
import ua.demo.agentlab.ui.UiTestScenario;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiPageObjectPromptBuildStage {

    private final AiPageObjectPromptBuilder promptBuilder;

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
        List<UiTestScenario> pageScenarios = scope.pageScenarios();
        metadata.put("schemaVersion", LlmOutputSchemaVersion.AI_PAGE_OBJECT_SPEC);
        metadata.put("pageName", scope.pageName());
        metadata.put("scopedScenarioCount", pageScenarios.size());
        metadata.put("scopedScenarioIds", pageScenarios.stream().map(UiTestScenario::id).toList());
        metadata.put("hasBaselineSpec", scope.baselineSpec() != null);
        metadata.put("mappedPageCount", scope.scopedContext() == null || scope.scopedContext().mappedUiKnowledge() == null
                ? 0
                : scope.scopedContext().mappedUiKnowledge().pages().size());
        metadata.put("promptAllowedLocatorCount", scope.scopedContext() == null || scope.scopedContext().promptUiEvidence() == null
                ? 0
                : scope.scopedContext().promptUiEvidence().requiredLocators().size());
        metadata.put("promptActionCount", scope.scopedContext() == null || scope.scopedContext().promptUiEvidence() == null
                ? 0
                : scope.scopedContext().promptUiEvidence().requiredActions().size());
        metadata.put("promptAssertionCount", scope.scopedContext() == null || scope.scopedContext().promptUiEvidence() == null
                ? 0
                : scope.scopedContext().promptUiEvidence().requiredAssertions().size());
        metadata.put("pageModelPageCount", scope.scopedContext() == null || scope.scopedContext().pageModelBundle() == null
                ? 0
                : scope.scopedContext().pageModelBundle().pages().size());
        metadata.put("canonicalCaseCount", scope.scopedContext() == null || scope.scopedContext().canonicalTestCaseBundle() == null
                ? 0
                : scope.scopedContext().canonicalTestCaseBundle().testCases().size());
        return metadata;
    }
}
