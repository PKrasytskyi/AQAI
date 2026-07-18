package ua.demo.agentlab.ui.discovery.pagemodel.stage;

import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;

import java.util.List;

/** Normalizes raw discovery facts without assigning semantic meaning. */
public final class RawElementNormalizer {

    public List<RawElement> normalize(List<RawElement> elements) {
        if (elements == null || elements.isEmpty()) return List.of();
        return elements.stream().filter(java.util.Objects::nonNull).toList();
    }
}
