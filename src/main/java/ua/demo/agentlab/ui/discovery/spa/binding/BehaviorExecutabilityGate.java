package ua.demo.agentlab.ui.discovery.spa.binding;

import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorAssertion;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorStep;

import java.util.ArrayList;
import java.util.List;

/** Decides executability after semantic, data, and postcondition binding have completed. */
public final class BehaviorExecutabilityGate {
    public ExecutabilityDecision evaluate(StructuredBehaviorContract contract, List<BoundSpaBehaviorStep> steps,
                                          List<BoundSpaBehaviorAssertion> assertions, List<String> existingReasons) {
        List<String> reasons = new ArrayList<>(existingReasons == null ? List.of() : existingReasons);
        boolean actionRequired = contract.actions().stream()
                .anyMatch(action -> !normalize(action).startsWith("inspect"));
        if (actionRequired && steps.isEmpty()) reasons.add("No confirmed executable action binding was produced.");
        if (assertions.stream().anyMatch(assertion -> !assertion.verifiable()))
            reasons.add("One or more assertion targets have no confirmed locator or route binding.");
        List<String> unique = reasons.stream().distinct().toList();
        return new ExecutabilityDecision(unique.isEmpty() && (!actionRequired || !steps.isEmpty()), unique);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public record ExecutabilityDecision(boolean executable, List<String> reviewReasons) {}
}
