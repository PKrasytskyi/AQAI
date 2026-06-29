package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.orchestration.WorkflowState;

public interface WorkflowStatePipelineAdapter<O> {

    void applyOutput(O output, WorkflowState state);
}
