package ua.demo.agentlab.ui.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.ui.writer.PageObjectWriter;

import java.util.List;
import java.util.Set;

public class PageObjectWriterAgent implements WorkflowAgent,
        PipelineAgent<UiTestPlan, List<GeneratedSourceFile>> {

    private final PageObjectWriter writer;

    public PageObjectWriterAgent(PageObjectWriter writer){
        this.writer = writer;
    }

    @Override
    public String name() {
        return "page-object-writer-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.UI_TEST_PLAN);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.PAGE_OBJECT_FILES);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.UI_TEST_PLAN;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.PAGE_OBJECT_FILES;
    }

    @Override
    public List<GeneratedSourceFile> execute(UiTestPlan input, WorkflowRunEnvelope run) {
        return writer.write(input);
    }

    @Override
    public void applyOutput(List<GeneratedSourceFile> files, WorkflowState state) {
        state.setPageObjectFiles(files);
        state.addArtifact("ui.page.objects.count", String.valueOf(files.size()));
        state.addFinding("Page objects generated: " + files.size());
    }
}
