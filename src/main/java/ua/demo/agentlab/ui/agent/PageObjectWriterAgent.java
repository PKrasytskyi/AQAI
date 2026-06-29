package ua.demo.agentlab.ui.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.ui.writer.PageObjectWriter;

import java.util.List;

public class PageObjectWriterAgent implements WorkflowAgent {

    private final PageObjectWriter writer;

    public PageObjectWriterAgent(PageObjectWriter writer){
        this.writer = writer;
    }

    @Override
    public String name() {
        return "page-object-writer-agent";
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
        List<GeneratedSourceFile> files = writer.write(state.getUiTestPlan());
        state.setPageObjectFiles(files);
        state.addArtifact("ui.page.objects.count", String.valueOf(files.size()));
        state.addFinding("Page objects generated: " + files.size());
    }
}
