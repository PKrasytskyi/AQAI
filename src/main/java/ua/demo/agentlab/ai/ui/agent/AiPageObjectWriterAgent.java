package ua.demo.agentlab.ai.ui.agent;

import ua.demo.agentlab.ai.ui.writer.AiPageObjectTemplateWriter;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;

public class AiPageObjectWriterAgent implements WorkflowAgent {

    private final AiPageObjectTemplateWriter aiWriter;

    public AiPageObjectWriterAgent(AiPageObjectTemplateWriter aiWriter) {
        if (aiWriter == null) {
            throw new IllegalArgumentException("writers cannot be null");
        }
        this.aiWriter = aiWriter;
    }

    @Override
    public String name() {
        return "ai-page-object-writer-agent";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getUiTestPlan() != null && state.getPageObjectFiles().isEmpty();
    }

    @Override
    public void execute(WorkflowState state) {
        List<GeneratedSourceFile> aiFiles = state.getAiPageObjectSpecs().isEmpty()
                ? List.of()
                : aiWriter.write(state.getAiPageObjectSpecs());
        if (aiFiles.isEmpty()) {
            state.fail("Pure AI mode did not produce any page object files");
            return;
        }
        state.setPageObjectFiles(aiFiles);
        state.addArtifact("ui.page.objects.count", String.valueOf(aiFiles.size()));
        state.addArtifact("ui.page.objects.ai.override.count", String.valueOf(aiFiles.size()));
        state.addFinding("Pure AI page objects generated: " + aiFiles.size());
    }
}
