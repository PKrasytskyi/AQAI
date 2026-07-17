package ua.demo.agentlab.ui.discovery.evidence.funnel;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBindingBundle;

import java.util.List;

public record UiEvidenceFunnelInput(
        String runId,
        List<StructuredBehaviorContract> requirements,
        SpaInventoryBundle inventory,
        List<BoundSpaBehaviorContract> bindings,
        SpaLiveTargetedVerificationResult liveVerification,
        TargetStateBindingBundle targetStateBindings,
        AiContextPackage aiContext
) {
    public UiEvidenceFunnelInput {
        runId = runId == null ? "" : runId.trim();
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        bindings = bindings == null ? List.of() : List.copyOf(bindings);
        targetStateBindings = targetStateBindings == null
                ? new TargetStateBindingBundle(TargetStateBindingBundle.SCHEMA_VERSION, null, List.of(), List.of(),
                List.of("ui-evidence-funnel:no-target-state-bindings"))
                : targetStateBindings;
    }
}
