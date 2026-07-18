package ua.demo.agentlab.ui.discovery.evidence.funnel;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTransitionDiscovery;
import ua.demo.agentlab.ui.discovery.interaction.observability.EvidenceProjectionTrace;

import java.util.List;

public record UiEvidenceFunnelInput(
        String runId,
        List<StructuredBehaviorContract> requirements,
        SpaInventoryBundle inventory,
        SourceStateBindingBundle sourceStateBindings,
        LiveTransitionDiscovery liveTransitionDiscovery,
        List<BoundSpaBehaviorContract> bindings,
        SpaLiveTargetedVerificationResult liveVerification,
        TargetStateBindingBundle targetStateBindings,
        AiContextPackage aiContext,
        EvidenceProjectionTrace projectionTrace
) {
    public UiEvidenceFunnelInput {
        runId = runId == null ? "" : runId.trim();
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        sourceStateBindings = sourceStateBindings == null
                ? new SourceStateBindingBundle(SourceStateBindingBundle.SCHEMA_VERSION, null, List.of(),
                List.of("ui-evidence-funnel:no-source-state-bindings"))
                : sourceStateBindings;
        liveTransitionDiscovery = liveTransitionDiscovery == null
                ? new LiveTransitionDiscovery(LiveTransitionDiscovery.SCHEMA_VERSION, null, liveVerification,
                List.of(), List.of("ui-evidence-funnel:no-live-transition-discovery"))
                : liveTransitionDiscovery;
        bindings = bindings == null ? List.of() : List.copyOf(bindings);
        targetStateBindings = targetStateBindings == null
                ? new TargetStateBindingBundle(TargetStateBindingBundle.SCHEMA_VERSION, null, List.of(), List.of(),
                List.of("ui-evidence-funnel:no-target-state-bindings"))
                : targetStateBindings;
    }

    public UiEvidenceFunnelInput(
            String runId,
            List<StructuredBehaviorContract> requirements,
            SpaInventoryBundle inventory,
            SourceStateBindingBundle sourceStateBindings,
            LiveTransitionDiscovery liveTransitionDiscovery,
            List<BoundSpaBehaviorContract> bindings,
            SpaLiveTargetedVerificationResult liveVerification,
            TargetStateBindingBundle targetStateBindings,
            AiContextPackage aiContext
    ) {
        this(runId, requirements, inventory, sourceStateBindings, liveTransitionDiscovery, bindings,
                liveVerification, targetStateBindings, aiContext, null);
    }

    public UiEvidenceFunnelInput(
            String runId,
            List<StructuredBehaviorContract> requirements,
            SpaInventoryBundle inventory,
            List<BoundSpaBehaviorContract> bindings,
            SpaLiveTargetedVerificationResult liveVerification,
            TargetStateBindingBundle targetStateBindings,
            AiContextPackage aiContext
    ) {
        this(runId, requirements, inventory, null, null, bindings, liveVerification, targetStateBindings, aiContext, null);
    }
}
