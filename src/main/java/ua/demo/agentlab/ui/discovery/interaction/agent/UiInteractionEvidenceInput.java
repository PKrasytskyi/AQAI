package ua.demo.agentlab.ui.discovery.interaction.agent;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;

import java.util.List;

public record UiInteractionEvidenceInput(
        UiInteractionInventory inventory,
        SpaLiveTargetedVerificationResult liveVerification,
        List<StructuredBehaviorContract> requirements,
        List<AssertionContract> assertions
) {
    public UiInteractionEvidenceInput {
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
    }
}
