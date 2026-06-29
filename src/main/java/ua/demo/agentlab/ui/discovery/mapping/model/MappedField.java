package ua.demo.agentlab.ui.discovery.mapping.model;

import java.util.List;

public record MappedField(
        String fieldId,
        String fieldName,
        String fieldType,
        String label,
        boolean required,
        String placeholder,
        List<LocatorCandidate> locatorCandidates
) {
    public MappedField {
        fieldId = fieldId == null ? "" : fieldId.trim();
        fieldName = fieldName == null ? "" : fieldName.trim();
        fieldType = fieldType == null ? "" : fieldType.trim();
        label = label == null ? "" : label.trim();
        placeholder = placeholder == null ? "" : placeholder.trim();
        locatorCandidates = locatorCandidates == null ? List.of() : List.copyOf(locatorCandidates);
    }
}
