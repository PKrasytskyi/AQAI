package ua.demo.agentlab.requirements.normalization.model;

import java.util.List;

public record NormalizedRequirementBundle(

        String source,
        List<NormalizedRequirement> requirements,
        List<String> assumptions,
        List<String> risks
) {}
