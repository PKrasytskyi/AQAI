package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;

import java.util.List;

public record SpaTargetStateBindingOutput(TargetStateBindingBundle bindings, SpaInventoryBundle effectiveInventory,
                                          SourceStateBindingBundle reboundSources, List<String> artifacts) {
    public SpaTargetStateBindingOutput {
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
