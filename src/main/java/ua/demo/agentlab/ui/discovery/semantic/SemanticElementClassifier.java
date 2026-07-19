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
        String structuralEvidence = normalize(String.join(" ",
                element.technicalType(), element.semanticType(), element.tag(), element.inputType(), element.role()
        ));
        String identityEvidence = normalize(String.join(" ",
                element.name(), element.id(), element.placeholder(), element.ariaLabel()
        ));
        if (containsAny(structuralEvidence + " " + identityEvidence,
                "_token", "csrf", "xsrf", "authenticity_token")) {
            return "SYSTEM_HIDDEN";
        }
        String tag = normalize(element.tag());
        String inputType = normalize(element.inputType());
        String role = normalize(element.role());

        // Control type is structural. Visible text such as a heading named "Checkboxes" or
        // "Dropdown" must never turn a non-interactive element into an executable control.
        if ("input".equals(tag) && "password".equals(inputType)) {
            return "PASSWORD_INPUT";
        }
        if ("input".equals(tag) && "file".equals(inputType)) {
            return "FILE_INPUT";
        }
        if (("input".equals(tag) && "checkbox".equals(inputType)) || "checkbox".equals(role)) {
            return "CHECKBOX";
        }
        if (("input".equals(tag) && "radio".equals(inputType)) || "radio".equals(role)) {
            return "RADIO";
        }
        if ("input".equals(tag) && "range".equals(inputType)) {
            return "RANGE_SLIDER";
        }
        if ("select".equals(tag) || "combobox".equals(role)) {
            return "SELECT";
        }
        if ("textarea".equals(tag)) {
            return "TEXTAREA";
        }
        if ("button".equals(tag) || "button".equals(role)
                || "input".equals(tag) && ("submit".equals(inputType) || "button".equals(inputType))) {
            return "BUTTON";
        }
        if ("a".equals(tag) || "link".equals(role)) {
            return "LINK";
        }
        if ("img".equals(tag) || "image".equals(role)) {
            return "IMAGE";
        }
        if ("input".equals(tag) && !"hidden".equals(inputType)) {
            return "INPUT";
        }
        if (tag.matches("h[1-6]") || "heading".equals(role)) {
            return "HEADING";
        }
        if (containsAny(structuralEvidence, "table", "grid", "list", "collection", "card")) {
            return "COLLECTION";
        }
        if (containsAny(structuralEvidence, "input", "field", "textbox", "search")) {
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
