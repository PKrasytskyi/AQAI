package ua.demo.agentlab.requirements.normalization.model;

import java.util.List;

public record NormalizedRequirement(

        String id,
        String title,
        String statement,
        String expectedResult,
        boolean uiRelevant,
        boolean apiRelevant,
        List<String> tags,
        SourceReference sourceReference
) {
}
