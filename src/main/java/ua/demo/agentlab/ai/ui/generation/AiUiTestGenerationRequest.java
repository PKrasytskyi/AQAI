package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.UiTestPlan;

import java.util.List;

public record AiUiTestGenerationRequest(
        WorkflowRunEnvelope runEnvelope,
        AiContextPackage contextPackage,
        UiTestPlan uiTestPlan,
        List<AiPageObjectSpec> pageObjectSpecs,
        List<AiUiTestSpec> baselineSpecs
) {
    public AiUiTestGenerationRequest {
        pageObjectSpecs = pageObjectSpecs == null ? List.of() : List.copyOf(pageObjectSpecs);
        baselineSpecs = baselineSpecs == null ? List.of() : List.copyOf(baselineSpecs);
    }
}
