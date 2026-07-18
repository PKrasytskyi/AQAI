package ua.demo.agentlab.ui.discovery.spa.binding;

import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.*;

import java.util.List;
import java.util.Map;

/** Materializes a binding result without making matching decisions. */
public final class BehaviorBindingResultAssembler {
    public BoundSpaBehaviorContract assemble(StructuredBehaviorContract source, SpaPageInventory page, String flowId,
                                             List<SemanticComponentInventory> components,
                                             List<BoundSpaBehaviorStep> steps,
                                             List<BoundSpaBehaviorAssertion> assertions,
                                             Map<String, String> values,
                                             BehaviorExecutabilityGate.ExecutabilityDecision decision) {
        return new BoundSpaBehaviorContract(source.requirementId(), source.capability(), page.pageId(), page.route(), flowId,
                components.stream().map(SemanticComponentInventory::componentId).toList(), steps, assertions, values,
                decision.executable(), decision.reviewReasons());
    }

    public BoundSpaBehaviorContract empty(StructuredBehaviorContract source, Map<String, String> values,
                                          List<String> review) {
        return new BoundSpaBehaviorContract(source.requirementId(), source.capability(), "", "", "", List.of(),
                List.of(), List.of(), values, false, review.stream().distinct().toList());
    }
}
