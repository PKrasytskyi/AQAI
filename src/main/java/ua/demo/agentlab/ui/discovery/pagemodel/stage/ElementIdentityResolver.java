package ua.demo.agentlab.ui.discovery.pagemodel.stage;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;

import java.util.List;
import java.util.Locale;

/** Owns stable element names and raw-to-semantic identity keys. */
public final class ElementIdentityResolver {

    public String uniqueElementId(List<PageElementModel> elements, String pageId, String semanticName) {
        String base = buildElementId(pageId, semanticName);
        String candidate = base;
        int suffix = 2;
        while (containsElement(elements, candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean containsElement(List<PageElementModel> elements, String candidate) {
        return elements.stream().anyMatch(element -> element.elementId().equals(candidate));
    }

    public String buildElementId(String pageId, String semanticName) {
        return sanitize(pageId) + ":element:" + sanitize(semanticName);
    }

    public String rawElementKey(String type, String text, String id, String name, String href) {
        return String.join("|", normalize(type), normalize(text), normalize(id), normalize(name), normalize(href));
    }

    public String semanticName(String value) {
        String sanitized = sanitize(value);
        if (sanitized.isBlank()) return "element";
        String[] parts = sanitized.split("-");
        StringBuilder result = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            if (!parts[index].isBlank()) {
                result.append(Character.toUpperCase(parts[index].charAt(0))).append(parts[index].substring(1));
            }
        }
        return result.toString();
    }

    public String sanitize(String value) {
        return normalize(value).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
