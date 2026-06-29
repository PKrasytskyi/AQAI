package ua.demo.agentlab.ui.discovery.selenium.model;

import java.util.List;

public record DiscoveredForm(
        String formId,
        String formName,
        String action,
        List<DiscoveredField> fields,
        List<DiscoveredInteractiveElement> submitActions
) {
}
