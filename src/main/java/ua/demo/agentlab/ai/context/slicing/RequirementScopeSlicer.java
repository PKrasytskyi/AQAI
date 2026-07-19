package ua.demo.agentlab.ai.context.slicing;

import ua.demo.agentlab.ai.context.AiContextScope;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

import java.util.List;

/** Slices requirements only by explicit requirement identity. */
public final class RequirementScopeSlicer {

    public NormalizedRequirementBundle slice(NormalizedRequirementBundle source, AiContextScope scope) {
        if (source == null) return null;
        List<NormalizedRequirement> filtered = source.requirements().stream()
                .filter(requirement -> scope.targetRequirementIds().isEmpty()
                        || scope.targetRequirementIds().contains(requirement.id()))
                .toList();
        if (filtered.isEmpty()) filtered = source.requirements();
        return new NormalizedRequirementBundle(source.source(), filtered, source.assumptions(), source.risks());
    }
}
