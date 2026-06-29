package ua.demo.agentlab.ai.ui.agent;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.quality.AiRunQualitySummaryInput;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.orchestration.pipeline.WorkflowPipelineSnapshot;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.UiTestPlan;

import java.util.List;
import java.util.Map;

public record AiPageObjectSpecInput(
        WorkflowRunEnvelope runEnvelope,
        UiTestPlan uiTestPlan,
        AiContextPackage aiContextPackage,
        List<AiPageObjectSpec> baselineSpecs,
        AiRunQualitySummaryInput qualitySummaryInput,
        WorkflowPipelineSnapshot pipelineSnapshot,
        Map<String, String> artifacts
) {
    public AiPageObjectSpecInput {
        baselineSpecs = baselineSpecs == null ? List.of() : List.copyOf(baselineSpecs);
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
    }
}
