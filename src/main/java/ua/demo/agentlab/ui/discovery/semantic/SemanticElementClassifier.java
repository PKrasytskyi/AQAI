package ua.demo.agentlab.ui.discovery.semantic;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;

import java.util.Locale;

public class SemanticElementClassifier {

    public String classify(PageElementModel element) {
        if (element == null) {
            return "UNKNOWN";
        }
        if (!element.visible()
                || "hidden".equalsIgnoreCase(element.inputType())
                || element.attributes().containsKey("hidden")
                || "true".equalsIgnoreCase(element.attributes().get("aria-hidden"))) {
            return "SYSTEM_HIDDEN";
        }
        String evidence = normalize(String.join(" ",
                element.technicalType(),
                element.semanticType(),
                element.tag(),
                element.inputType(),
                element.role(),
                element.name(),
                element.id(),
                element.placeholder(),
                element.text()
        ));
        if (containsAny(evidence, "_token", "csrf", "xsrf", "authenticity_token")) {
            return "SYSTEM_HIDDEN";
        }
        if (containsAny(evidence, "password")) {
            return "PASSWORD_INPUT";
        }
        if (containsAny(evidence, "file")) {
            return "FILE_INPUT";
        }
        if (containsAny(evidence, "checkbox")) {
            return "CHECKBOX";
        }
        if (containsAny(evidence, "radio")) {
            return "RADIO";
        }
        if (containsAny(evidence, "select", "dropdown", "combobox")) {
            return "SELECT";
        }
        if (containsAny(evidence, "textarea")) {
            return "TEXTAREA";
        }
        if (containsAny(evidence, "button", "submit")) {
            return "BUTTON";
        }
        if (containsAny(evidence, "anchor", " link ", "navigation-link") || "a".equalsIgnoreCase(element.tag())) {
            return "LINK";
        }
        if (containsAny(evidence, "table", "grid", "list", "collection", "card")) {
            return "COLLECTION";
        }
        if (containsAny(evidence, "input", "field", "textbox", "search")) {
            return "INPUT";
        }
        return element.tag().isBlank() ? "UNKNOWN" : element.tag().toUpperCase(Locale.ROOT);
    }

    public String semanticName(PageElementModel element) {
        if (element == null) {
            return "element";
        }
        return fieldHint(firstNonBlank(
                element.name(),
                element.id(),
                element.ariaLabel(),
                element.placeholder(),
                element.text(),
                element.semanticType(),
                element.technicalType(),
                element.elementId(),
                "element"
        ));
    }

    private String fieldHint(String value) {
        String normalized = value == null ? "" : value.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "element";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            builder.append(parts[index].substring(0, 1).toUpperCase()).append(parts[index].substring(1));
        }
        return builder.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
