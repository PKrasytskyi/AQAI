package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.ui.discovery.spa.model.ComponentInteractionGraph;

import java.util.List;

public record SpaComponentInteractionGraphOutput(ComponentInteractionGraph graph, String persistenceDetails, List<String> artifacts) {
    public SpaComponentInteractionGraphOutput {
        persistenceDetails = persistenceDetails == null ? "" : persistenceDetails.trim();
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
