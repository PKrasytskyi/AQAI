package ua.demo.agentlab.ui.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.ui.writer.UiTestWriter;

import java.util.List;

public class LayeredUiTestWriterAgent implements WorkflowAgent {

    private final UiTestWriter writer;

    public LayeredUiTestWriterAgent(UiTestWriter writer){
        this.writer = writer;
    }

    @Override
    public String name() {
        return "layered-ui-test-writer-agent";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getUiTestPlan() != null && !state.getPageObjectFiles().isEmpty()
                && state.getUiTestFiles().isEmpty();
    }

    @Override
    public void execute(WorkflowState state) {
        List<GeneratedSourceFile> files = writer.write(state.getUiTestPlan());
        state.setUiTestFiles(files);
        state.addArtifact("ui.test.count", String.valueOf(files.size()));
        state.addFinding("Layered UI test file generated: " + files.size());
    }
}
