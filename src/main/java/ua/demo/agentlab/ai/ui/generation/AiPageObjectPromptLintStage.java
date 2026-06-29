package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.ui.prompt.quality.PromptQualityLinter;
import ua.demo.agentlab.ai.ui.prompt.quality.PromptQualityReport;

import java.util.ArrayList;
import java.util.List;

public class AiPageObjectPromptLintStage {

    private final PromptQualityLinter promptQualityLinter;

    public AiPageObjectPromptLintStage() {
        this(new PromptQualityLinter());
    }

    AiPageObjectPromptLintStage(PromptQualityLinter promptQualityLinter) {
        if (promptQualityLinter == null) {
            throw new IllegalArgumentException("promptQualityLinter cannot be null");
        }
        this.promptQualityLinter = promptQualityLinter;
    }

    public PromptQualityReport validate(AiPageObjectPromptDraft draft) {
        if (draft == null || draft.scope() == null) {
            throw new IllegalArgumentException("prompt draft cannot be null");
        }
        validateScope(draft.scope());
        PromptQualityReport report = promptQualityLinter.validate(
                draft.prompt(),
                draft.scope().pageName(),
                draft.scope().scopedContext(),
                draft.scope().pageScenarios(),
                draft.scope().baselineSpec()
        );
        return report;
    }

    public List<String> scopeFindings(AiPageObjectPromptScope scope) {
        if (scope == null) {
            return List.of();
        }
        List<String> findings = new ArrayList<>();
        int totalScenarioCount = scope.originalContext() == null || scope.originalContext().uiTestPlan() == null
                ? 0
                : scope.originalContext().uiTestPlan().scenarios().size();
        int planPageCount = scope.originalContext() == null || scope.originalContext().uiTestPlan() == null
                ? 0
                : scope.originalContext().uiTestPlan().pageNames().size();
        int mappedPageCount = scope.scopedContext() == null || scope.scopedContext().mappedUiKnowledge() == null
                ? 0
                : scope.scopedContext().mappedUiKnowledge().pages().size();
        int pageModelCount = scope.scopedContext() == null || scope.scopedContext().pageModelBundle() == null
                ? 0
                : scope.scopedContext().pageModelBundle().pages().size();
        if (mappedPageCount == 0 && pageModelCount == 0) {
            findings.add("Page scope for " + scope.pageName()
                    + " has no discovery evidence; continuing with deterministic baseline page object spec");
        }
        if (planPageCount > 1 && totalScenarioCount > 0 && scope.pageScenarios().size() == totalScenarioCount) {
            findings.add("Page scope for " + scope.pageName()
                    + " matched every UI scenario; verify this is intentional for cross-page coverage");
        }
        return findings;
    }

    private void validateScope(AiPageObjectPromptScope scope) {
        int mappedPageCount = scope.scopedContext() == null || scope.scopedContext().mappedUiKnowledge() == null
                ? 0
                : scope.scopedContext().mappedUiKnowledge().pages().size();
        int pageModelCount = scope.scopedContext() == null || scope.scopedContext().pageModelBundle() == null
                ? 0
                : scope.scopedContext().pageModelBundle().pages().size();
        if (scope.pageScenarios().isEmpty()) {
            throw new IllegalStateException("Page scope for " + scope.pageName() + " has no matching UI scenarios");
        }
        if (mappedPageCount == 0 && pageModelCount == 0 && scope.baselineSpec() == null) {
            throw new IllegalStateException("Page scope for " + scope.pageName() + " has no mapped page evidence");
        }
    }
}
