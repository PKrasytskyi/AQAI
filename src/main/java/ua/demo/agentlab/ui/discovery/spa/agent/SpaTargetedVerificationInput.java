package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;

import java.util.List;

public record SpaTargetedVerificationInput(
        UiInteractionInventory inventory,
        CanonicalTestCaseBundle testCases,
        List<StructuredBehaviorContract> structuredContracts,
        SourceStateBindingBundle sourceBindings
) {
    public SpaTargetedVerificationInput(UiInteractionInventory inventory, CanonicalTestCaseBundle testCases) {
        this(inventory, testCases, List.of(), null);
    }
    public SpaTargetedVerificationInput(UiInteractionInventory inventory, CanonicalTestCaseBundle testCases,
                                        List<StructuredBehaviorContract> structuredContracts) {
        this(inventory, testCases, structuredContracts, null);
    }
}
