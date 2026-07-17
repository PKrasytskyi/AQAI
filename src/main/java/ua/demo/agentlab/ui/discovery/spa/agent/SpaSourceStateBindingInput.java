package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;

import java.util.List;

public record SpaSourceStateBindingInput(ProjectProfile profile, List<StructuredBehaviorContract> contracts,
                                         SpaInventoryBundle inventory) {
    public SpaSourceStateBindingInput {
        contracts = contracts == null ? List.of() : List.copyOf(contracts);
    }
}
