package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceLifecycleResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaBehaviorExecutionBundle;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTransitionDiscovery;

import java.util.List;

public record SpaLiveTargetedVerificationOutput(SpaLiveTargetedVerificationResult result,
                                                LiveTransitionDiscovery transitionDiscovery,
                                                SpaEvidenceLifecycleResult lifecycle,
                                                SpaBehaviorExecutionBundle behaviorExecution,
                                                String behaviorPromotion,
                                                String stateGraphPersistence,
                                                List<String> artifacts) {
    public SpaLiveTargetedVerificationOutput(SpaLiveTargetedVerificationResult result, SpaEvidenceLifecycleResult lifecycle, List<String> artifacts) {
        this(result, null, lifecycle, new SpaBehaviorExecutionBundle(SpaBehaviorExecutionBundle.SCHEMA_VERSION, null, List.of(), List.of("not-executed")), "skipped:not-executed", "skipped:not-executed", artifacts);
    }
    public SpaLiveTargetedVerificationOutput {
        stateGraphPersistence = stateGraphPersistence == null ? "" : stateGraphPersistence.trim();
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
