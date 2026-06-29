package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.GeneratedUiContractValidationResult;

import java.util.List;

public record GenerationPipelineStage(
        List<AiPageObjectSpec> aiPageObjectSpecs,
        List<AiUiTestSpec> aiUiTestSpecs,
        List<GeneratedSourceFile> pageObjectFiles,
        List<GeneratedSourceFile> uiTestFiles,
        GeneratedUiContractValidationResult generatedUiContractValidationResult,
        GeneratedCodeValidationResult generatedCodeValidationResult
) {
    public GenerationPipelineStage {
        aiPageObjectSpecs = aiPageObjectSpecs == null ? List.of() : List.copyOf(aiPageObjectSpecs);
        aiUiTestSpecs = aiUiTestSpecs == null ? List.of() : List.copyOf(aiUiTestSpecs);
        pageObjectFiles = pageObjectFiles == null ? List.of() : List.copyOf(pageObjectFiles);
        uiTestFiles = uiTestFiles == null ? List.of() : List.copyOf(uiTestFiles);
    }
}
