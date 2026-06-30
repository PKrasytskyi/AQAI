package ua.demo.agentlab.ui.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.ui.writer.UiTestWriter;

import java.util.List;
import java.util.Set;

public class LayeredUiTestWriterAgent implements WorkflowAgent,
        PipelineAgent<UiTestPlan, List<GeneratedSourceFile>> {

    private final UiTestWriter writer;

    public LayeredUiTestWriterAgent(UiTestWriter writer){
        this.writer = writer;
    }

    @Override
    public String name() {
        return "layered-ui-test-writer-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.UI_TEST_PLAN, WorkflowArtifact.PAGE_OBJECT_FILES);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.UI_TEST_FILES);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.UI_TEST_PLAN;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.UI_TEST_FILES;
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return PipelineAgent.super.supports(store, state)
                && store.get(WorkflowArtifact.PAGE_OBJECT_FILES).isPresent();
    }

    @Override
    public List<GeneratedSourceFile> execute(UiTestPlan input, WorkflowRunEnvelope run) {
        return writer.write(input);
    }

    @Override
    public void applyOutput(List<GeneratedSourceFile> files, WorkflowState state) {
        state.setUiTestFiles(files);
        state.addArtifact("ui.test.count", String.valueOf(files.size()));
        state.addFinding("Layered UI test file generated: " + files.size());
    }
}
