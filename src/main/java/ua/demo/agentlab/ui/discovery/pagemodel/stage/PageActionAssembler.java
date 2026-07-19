package ua.demo.agentlab.ui.discovery.pagemodel.stage;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageActionModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Converts classified element semantics into page-owned action candidates. */
public final class PageActionAssembler {

    private final ElementIdentityResolver identities;

    public PageActionAssembler(ElementIdentityResolver identities) {
        this.identities = identities;
    }

    public List<PageActionModel> fromClassification(
            String elementId,
            List<String> actions,
            double confidence
    ) {
        return actions == null ? List.of() : actions.stream()
                .map(action -> action(elementId, action, toActionName(action),
                        "Inferred from browser DOM technical and semantic classification", confidence))
                .toList();
    }

    public List<PageActionModel> infer(String elementId, String technicalType, String text, String href,
                                       boolean enabled) {
        if (!enabled) return List.of();
        Set<String> types = new LinkedHashSet<>();
        String normalized = normalize(text + " " + href + " " + technicalType);
        if (technicalType.contains("LINK") || href != null && !href.isBlank()) {
            if (containsAny(normalized, "detail", "details", "view", "record")) {
                types.add("open-details");
            } else if (containsAny(normalized, "cart", "basket", "bag", "container")) {
                types.add("open-target-container");
            } else {
                types.add("click");
            }
        } else if (technicalType.contains("BUTTON")) {
            if (containsAny(normalized, "remove", "delete", "clear")) {
                types.add("remove-entity-from-container");
            } else if (containsAny(normalized, "add", "cart/add", "buy")) {
                types.add("add-entity-to-container");
            } else if (containsAny(normalized, "search")) {
                types.add("search");
            } else {
                types.add("click");
            }
        } else if (technicalType.contains("INPUT")) {
            types.add("type");
            types.add("clear");
        } else if (!technicalType.contains("UNKNOWN")) {
            types.add("click");
        }
        return types.stream().map(type -> action(elementId, type, toActionName(type),
                "Inferred from technical type and visible element evidence", 0.72d)).toList();
    }

    public List<PageActionModel> forField(String elementId, String fieldType) {
        String normalized = normalize(fieldType);
        return switch (normalized) {
            case "select" -> List.of(action(elementId, "select", "selectValue", "Inferred from select field", 0.86d));
            case "checkbox" -> List.of(
                    action(elementId, "check", "check", "Inferred from checkbox field", 0.86d),
                    action(elementId, "uncheck", "uncheck", "Inferred from checkbox field", 0.86d));
            case "radio" -> List.of(action(elementId, "select", "selectRadio", "Inferred from radio field", 0.86d));
            case "file" -> List.of(action(elementId, "upload", "uploadFile", "Inferred from file input", 0.86d));
            default -> List.of(
                    action(elementId, "type", "typeText", "Inferred from input field", 0.86d),
                    action(elementId, "clear", "clear", "Inferred from input field", 0.80d));
        };
    }

    private PageActionModel action(String elementId, String type, String name, String description, double confidence) {
        return new PageActionModel(elementId + ":action:" + identities.sanitize(type), type, name, description, confidence);
    }

    private String toActionName(String actionType) {
        String[] parts = identities.sanitize(actionType).split("-");
        if (parts.length == 0) return "interact";
        StringBuilder name = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            if (!parts[index].isBlank()) name.append(Character.toUpperCase(parts[index].charAt(0))).append(parts[index].substring(1));
        }
        return name.toString();
    }

    private boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) if (text.contains(fragment)) return true;
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
