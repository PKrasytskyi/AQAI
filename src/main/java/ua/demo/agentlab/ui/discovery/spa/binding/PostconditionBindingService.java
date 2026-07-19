package ua.demo.agentlab.ui.discovery.spa.binding;

import ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorAssertion;

import java.util.List;
import java.util.function.Function;

/** Binds typed postconditions independently from semantic actions and scenario data. */
public final class PostconditionBindingService {

    public List<BoundSpaBehaviorAssertion> bind(List<StructuredAssertionRequirement> assertions,
                                                Function<StructuredAssertionRequirement, BoundSpaBehaviorAssertion> resolver,
                                                List<String> review) {
        if (assertions == null || assertions.isEmpty()) return List.of();
        List<BoundSpaBehaviorAssertion> result = assertions.stream().map(resolver).toList();
        result.stream().filter(assertion -> !assertion.verifiable() && !assertion.reason().isBlank())
                .map(BoundSpaBehaviorAssertion::reason).forEach(review::add);
        return result;
    }
}
