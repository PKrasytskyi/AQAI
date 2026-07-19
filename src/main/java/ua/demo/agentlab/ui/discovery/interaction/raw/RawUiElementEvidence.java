package ua.demo.agentlab.ui.discovery.interaction.raw;

import java.util.List;
import java.util.Map;

/** Product-neutral raw UI facts captured before semantic classification. */
public record RawUiElementEvidence(
        String pageId,
        String route,
        String stateId,
        String containerId,
        String rawElementId,
        String tag,
        String inputType,
        String text,
        String idAttribute,
        String nameAttribute,
        String placeholder,
        String ariaLabel,
        String role,
        String href,
        String cssClass,
        boolean visible,
        boolean enabled,
        Map<String, String> attributes,
        List<RawLocatorObservation> locatorObservations,
        List<String> provenance
) {
    public RawUiElementEvidence {
        pageId = safe(pageId);
        route = safe(route);
        stateId = safe(stateId);
        containerId = safe(containerId);
        rawElementId = safe(rawElementId);
        tag = safe(tag);
        inputType = safe(inputType);
        text = safe(text);
        idAttribute = safe(idAttribute);
        nameAttribute = safe(nameAttribute);
        placeholder = safe(placeholder);
        ariaLabel = safe(ariaLabel);
        role = safe(role);
        href = safe(href);
        cssClass = safe(cssClass);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        locatorObservations = locatorObservations == null ? List.of() : List.copyOf(locatorObservations);
        provenance = provenance == null ? List.of() : List.copyOf(provenance);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
