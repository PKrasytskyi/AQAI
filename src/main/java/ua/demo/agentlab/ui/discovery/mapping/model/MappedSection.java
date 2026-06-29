package ua.demo.agentlab.ui.discovery.mapping.model;

import java.util.List;

public record MappedSection(
        String sectionId,
        String sectionName,
        String sectionType,
        List<String> elementIds
) {
    public MappedSection {
        sectionId = sectionId == null ? "" : sectionId.trim();
        sectionName = sectionName == null ? "" : sectionName.trim();
        sectionType = sectionType == null ? "" : sectionType.trim();
        elementIds = elementIds == null ? List.of() : List.copyOf(elementIds);
    }
}
