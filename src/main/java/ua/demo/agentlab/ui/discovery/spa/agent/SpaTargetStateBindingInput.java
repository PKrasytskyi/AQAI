package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTransitionDiscovery;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;

import java.util.List;

public record SpaTargetStateBindingInput(ProjectProfile profile, List<StructuredBehaviorContract> contracts,
                                         SpaInventoryBundle inventory, SourceStateBindingBundle sources,
                                         LiveTransitionDiscovery transitionDiscovery,
                                         SpaLiveTargetedVerificationResult liveVerification) {
    public SpaTargetStateBindingInput {
        contracts = contracts == null ? List.of() : List.copyOf(contracts);
    }
}
