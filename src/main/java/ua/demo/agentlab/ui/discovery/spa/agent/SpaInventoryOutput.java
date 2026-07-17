package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.ui.discovery.spa.SpaInventoryPersistenceResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;

import java.util.List;

public record SpaInventoryOutput(
        SpaInventoryBundle inventory,
        SpaInventoryPersistenceResult persistence,
        List<String> writtenFiles
) {
    public SpaInventoryOutput {
        writtenFiles = writtenFiles == null ? List.of() : List.copyOf(writtenFiles);
    }
}
