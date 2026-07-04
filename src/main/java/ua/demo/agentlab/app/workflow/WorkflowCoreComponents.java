package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.ai.context.RuleBasedCanonicalInteractionLayer;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.persistence.GeneratedFileWriter;
import ua.demo.agentlab.ui.selenium.writer.TemplateDrivenSeleniumWriter;
import ua.demo.agentlab.validation.GeneratedCodeValidator;

public record WorkflowCoreComponents(
        TemplateDrivenSeleniumWriter seleniumWriter,
        GeneratedFileWriter generatedFileWriter,
        GeneratedCodeValidator generatedCodeValidator,
        RuleBasedCanonicalInteractionLayer canonicalInteractionLayer,
        WorkflowAgent requirementReaderAgent,
        WorkflowAgent requirementNormalizationAgent,
        WorkflowAgent policyLoadingAgent,
        WorkflowAgent uiDiscoveryAgent,
        WorkflowAgent uiRuntimeEvidenceAgent,
        WorkflowAgent uiPageModelAgent,
        WorkflowAgent uiPageMappingAgent,
        WorkflowAgent uiPageKnowledgePersistenceAgent,
        WorkflowAgent uiDiscoveryArtifactPersistenceAgent,
        WorkflowAgent apiGenerationAgent,
        WorkflowAgent apiGeneratedSourcePersistenceAgent,
        WorkflowAgent flowScopedKnowledgeAgent,
        WorkflowAgent requirementToTestCaseAgent,
        WorkflowAgent uiTestPlanAgent,
        WorkflowAgent pageObjectWriterAgent,
        WorkflowAgent layeredUiTestWriterAgent,
        WorkflowAgent filePersistenceAgent,
        WorkflowAgent generatedUiContractValidationAgent,
        WorkflowAgent generatedCodeCompileAgent,
        WorkflowAgent generatedCodeReviewAgent
) {
}
