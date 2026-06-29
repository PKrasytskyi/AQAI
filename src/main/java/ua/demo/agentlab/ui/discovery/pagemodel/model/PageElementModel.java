package ua.demo.agentlab.ui.discovery.pagemodel.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PageElementModel(
        String elementId,
        String technicalType,
        String semanticType,
        String tag,
        String inputType,
        String text,
        String id,
        String name,
        String placeholder,
        String ariaLabel,
        String role,
        String href,
        String cssClass,
        boolean visible,
        boolean enabled,
        boolean required,
        Map<String, String> attributes,
        List<PageLocatorModel> locatorCandidates,
        PageLocatorModel bestLocator,
        List<PageActionModel> actions,
        double confidenceScore
) {
    public PageElementModel {
        elementId = elementId == null ? "" : elementId.trim();
        technicalType = technicalType == null ? "" : technicalType.trim();
        semanticType = semanticType == null ? "" : semanticType.trim();
        tag = tag == null ? "" : tag.trim();
        inputType = inputType == null ? "" : inputType.trim();
        text = text == null ? "" : text.trim();
        id = id == null ? "" : id.trim();
        name = name == null ? "" : name.trim();
        placeholder = placeholder == null ? "" : placeholder.trim();
        ariaLabel = ariaLabel == null ? "" : ariaLabel.trim();
        role = role == null ? "" : role.trim();
        href = href == null ? "" : href.trim();
        cssClass = cssClass == null ? "" : cssClass.trim();
        attributes = attributes == null ? Map.of() : copyAttributes(attributes);
        locatorCandidates = locatorCandidates == null ? List.of() : List.copyOf(locatorCandidates);
        if (bestLocator == null && !locatorCandidates.isEmpty()) {
            bestLocator = locatorCandidates.get(0);
        }
        actions = actions == null ? List.of() : List.copyOf(actions);
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
    }

    private static Map<String, String> copyAttributes(Map<String, String> source) {
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            copy.put(entry.getKey().trim(), entry.getValue().trim());
        }
        return Map.copyOf(copy);
    }
}
