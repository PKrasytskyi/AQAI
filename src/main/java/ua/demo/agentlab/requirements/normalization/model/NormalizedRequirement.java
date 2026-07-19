package ua.demo.agentlab.requirements.normalization.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record NormalizedRequirement(

        String id,
        String title,
        String statement,
        String expectedResult,
        boolean uiRelevant,
        boolean apiRelevant,
        List<String> tags,
        SourceReference sourceReference,
        List<StructuredAssertionRequirement> structuredAssertions,
        Map<String, List<String>> structuredSections
) {
    public NormalizedRequirement(String id, String title, String statement, String expectedResult,
                                 boolean uiRelevant, boolean apiRelevant, List<String> tags, SourceReference sourceReference) {
        this(id, title, statement, expectedResult, uiRelevant, apiRelevant, tags, sourceReference, List.of(), Map.of());
    }

    public NormalizedRequirement(String id, String title, String statement, String expectedResult,
                                 boolean uiRelevant, boolean apiRelevant, List<String> tags, SourceReference sourceReference,
                                 List<StructuredAssertionRequirement> structuredAssertions) {
        this(id, title, statement, expectedResult, uiRelevant, apiRelevant, tags, sourceReference, structuredAssertions, Map.of());
    }

    public NormalizedRequirement {
        structuredAssertions = structuredAssertions == null ? List.of() : List.copyOf(structuredAssertions);
        Map<String, List<String>> copiedSections = new LinkedHashMap<>();
        if (structuredSections != null) {
            structuredSections.forEach((section, values) -> copiedSections.put(
                    section == null ? "" : section.trim().toLowerCase(),
                    values == null ? List.of() : List.copyOf(values)
            ));
        }
        structuredSections = Map.copyOf(copiedSections);
    }
}
