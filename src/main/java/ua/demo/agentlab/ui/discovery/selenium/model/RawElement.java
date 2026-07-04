package ua.demo.agentlab.ui.discovery.selenium.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record RawElement(
        String rawElementId,
        String tag,
        String type,
        String text,
        String id,
        String name,
        String placeholder,
        String ariaLabel,
        String role,
        String href,
        String dataTestId,
        String cssClass,
        boolean visible,
        boolean enabled,
        boolean required,
        Map<String, String> attributes,
        Map<String, Integer> locatorMatchCounts,
        Map<String, Integer> locatorScopedMatchCounts,
        Map<String, String> locatorScopes
) {
    public RawElement(
            String rawElementId,
            String tag,
            String type,
            String text,
            String id,
            String name,
            String placeholder,
            String ariaLabel,
            String role,
            String href,
            String dataTestId,
            String cssClass,
            boolean visible,
            boolean enabled,
            boolean required,
            Map<String, String> attributes
    ) {
        this(
                rawElementId,
                tag,
                type,
                text,
                id,
                name,
                placeholder,
                ariaLabel,
                role,
                href,
                dataTestId,
                cssClass,
                visible,
                enabled,
                required,
                attributes,
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    public RawElement {
        rawElementId = rawElementId == null ? "" : rawElementId.trim();
        tag = tag == null ? "" : tag.trim();
        type = type == null ? "" : type.trim();
        text = text == null ? "" : text.trim();
        id = id == null ? "" : id.trim();
        name = name == null ? "" : name.trim();
        placeholder = placeholder == null ? "" : placeholder.trim();
        ariaLabel = ariaLabel == null ? "" : ariaLabel.trim();
        role = role == null ? "" : role.trim();
        href = href == null ? "" : href.trim();
        dataTestId = dataTestId == null ? "" : dataTestId.trim();
        cssClass = cssClass == null ? "" : cssClass.trim();
        attributes = attributes == null ? Map.of() : copy(attributes);
        locatorMatchCounts = locatorMatchCounts == null ? Map.of() : copyInteger(locatorMatchCounts);
        locatorScopedMatchCounts = locatorScopedMatchCounts == null ? Map.of() : copyInteger(locatorScopedMatchCounts);
        locatorScopes = locatorScopes == null ? Map.of() : copy(locatorScopes);
    }

    private static Map<String, String> copy(Map<String, String> source) {
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            copy.put(entry.getKey().trim(), entry.getValue().trim());
        }
        return Map.copyOf(copy);
    }

    private static Map<String, Integer> copyInteger(Map<String, Integer> source) {
        Map<String, Integer> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            copy.put(entry.getKey().trim(), Math.max(0, entry.getValue()));
        }
        return Map.copyOf(copy);
    }
}
