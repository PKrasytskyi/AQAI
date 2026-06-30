package ua.demo.agentlab.orchestration;

import java.util.Set;

public interface WorkflowAgent {

    String name();

    default Set<WorkflowArtifact> requires() {
        return Set.of();
    }

    default Set<WorkflowArtifact> produces() {
        return Set.of();
    }
}
