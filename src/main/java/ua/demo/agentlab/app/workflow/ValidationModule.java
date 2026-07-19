package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.validation.GeneratedCodeValidator;

public record ValidationModule(
        GeneratedCodeValidator generatedCodeValidator,
        WorkflowAgent generatedUiContractValidationAgent,
        WorkflowAgent generatedCodeCompileAgent,
        WorkflowAgent generatedCodeReviewAgent,
        WorkflowAgent generatedUiSmokeAgent,
        WorkflowAgent generatedTestExecutionAgent,
        WorkflowAgent uiSpaSmokeEvidenceFeedbackAgent,
        WorkflowAgent artifactLifecyclePromotionAgent,
        WorkflowAgent artifactReuseMetricsAgent,
        WorkflowAgent runtimeFeedbackDbUpdateAgent,
        WorkflowAgent runHistoryStatisticsAgent
) {
}
