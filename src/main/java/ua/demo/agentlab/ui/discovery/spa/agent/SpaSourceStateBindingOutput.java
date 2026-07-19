package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;

import java.util.List;

public record SpaSourceStateBindingOutput(SourceStateBindingBundle bindings, List<String> artifacts) {
    public SpaSourceStateBindingOutput {
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
