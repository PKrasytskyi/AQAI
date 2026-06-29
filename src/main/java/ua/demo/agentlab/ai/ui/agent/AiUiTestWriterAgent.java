package ua.demo.agentlab.ai.ui.agent;

import ua.demo.agentlab.ai.ui.writer.AiUiTestTemplateWriter;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;

public class AiUiTestWriterAgent implements WorkflowAgent {

    private final AiUiTestTemplateWriter aiWriter;

    public AiUiTestWriterAgent(AiUiTestTemplateWriter aiWriter) {
        if (aiWriter == null) {
            throw new IllegalArgumentException("writers cannot be null");
        }
        this.aiWriter = aiWriter;
    }

    @Override
    public String name() {
        return "ai-ui-test-writer-agent";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getUiTestPlan() != null
                && !state.getPageObjectFiles().isEmpty()
                && state.getUiTestFiles().isEmpty();
    }

    @Override
    public void execute(WorkflowState state) {
        List<GeneratedSourceFile> aiFiles = state.getAiUiTestSpecs().isEmpty()
                ? List.of()
                : aiWriter.write(state.getAiUiTestSpecs());
        if (aiFiles.isEmpty()) {
            state.fail("Pure AI mode did not produce any UI test files");
            return;
        }
        state.setUiTestFiles(aiFiles);
        state.addArtifact("ui.test.count", String.valueOf(aiFiles.size()));
        state.addArtifact("ui.test.ai.override.count", String.valueOf(aiFiles.size()));
        state.addFinding("Pure AI UI test files generated: " + aiFiles.size());
    }
}
