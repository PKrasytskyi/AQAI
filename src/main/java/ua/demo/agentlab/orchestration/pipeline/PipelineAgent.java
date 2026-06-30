package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;

public interface PipelineAgent<I, O> {

    WorkflowArtifact input();

    WorkflowArtifact output();

    @SuppressWarnings("unchecked")
    default I inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (store == null) {
            throw new IllegalArgumentException("store cannot be null");
        }
        return (I) store.require(input());
    }

    default boolean supports(I input, WorkflowRunEnvelope run) {
        return input != null;
    }

    default void applyOutput(O output, WorkflowState state) {
    }

    default boolean supports(PipelineArtifactStore store, WorkflowState state) {
        if (store == null) {
            return false;
        }
        if (store.get(output()).map(this::isProducedOutput).orElse(false)) {
            return false;
        }
        try {
            return supports(inputFrom(store, state), WorkflowRunEnvelope.from(state));
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    private boolean isProducedOutput(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof java.util.Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (value instanceof java.util.Map<?, ?> map) {
            return !map.isEmpty();
        }
        if (value instanceof String string) {
            return !string.isBlank();
        }
        return true;
    }

    O execute(I input, WorkflowRunEnvelope run);
}
