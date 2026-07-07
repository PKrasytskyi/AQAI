package ua.demo.agentlab.ui.discovery.selenium.classification;

import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ElementClassifier {

    public ElementClassification classify(String pageId, RawElement element) {
        if (element == null) {
            throw new IllegalArgumentException("element cannot be null");
        }
        String technicalType = technicalType(element);
        String semanticType = semanticType(element, technicalType);
        return new ElementClassification(
                pageId + ":element:" + sanitize(firstNonBlank(
                        element.dataTestId(),
                        element.id(),
                        element.name(),
                        element.ariaLabel(),
                        semanticType.toLowerCase(Locale.ROOT),
                        element.rawElementId()
                )),
                technicalType,
                semanticType,
                actions(technicalType, semanticType, element),
                confidence(technicalType, semanticType, element)
        );
    }

    public String technicalType(RawElement element) {
        String tag = normalize(element.tag());
        String type = normalize(element.type());
        String role = normalize(element.role());
        if ("input".equals(tag)) {
            return switch (type) {
                case "email" -> "EMAIL_INPUT";
                case "password" -> "PASSWORD_INPUT";
                case "checkbox" -> "CHECKBOX";
                case "radio" -> "RADIO";
                case "file" -> "FILE_INPUT";
                case "submit", "button" -> "BUTTON";
                default -> "INPUT";
            };
        }
        if ("button".equals(tag) || "button".equals(role)) {
            return "BUTTON";
        }
        if (tag.matches("h[1-6]")) {
            return "HEADING";
        }
        if ("a".equals(tag) && !element.href().isBlank()) {
            return "LINK";
        }
        if (("span".equals(tag) || "div".equals(tag))
                && containsAny(element.cssClass() + " " + element.ariaLabel() + " " + element.role(),
                "dropdown", "menu-trigger", "user-menu", "profile-menu")) {
            return "BUTTON";
        }
        if ("select".equals(tag)) {
            return "DROPDOWN";
        }
        if ("textarea".equals(tag)) {
            return "TEXTAREA";
        }
        if ("form".equals(tag)) {
            return "FORM";
        }
        if ("table".equals(tag)) {
            return "TABLE";
        }
        if ("tr".equals(tag)) {
            return "TABLE_ROW";
        }
        if ("td".equals(tag) || "th".equals(tag)) {
            return "TABLE_CELL";
        }
        if ("dialog".equals(tag) || "dialog".equals(role) || containsAny(element.cssClass(), "modal")) {
            return "MODAL";
        }
        if ("checkbox".equals(role)) {
            return "CHECKBOX";
        }
        if ("radio".equals(role)) {
            return "RADIO";
        }
        if (containsAny(element.cssClass() + " " + element.role(), "toast", "alert", "error", "success")) {
            return "MESSAGE_BLOCK";
        }
        return "CONTENT";
    }

    public String semanticType(RawElement element, String technicalType) {
        String text = normalize(String.join(" ",
                element.text(),
                element.id(),
                element.name(),
                element.placeholder(),
                element.ariaLabel(),
                element.href(),
                element.dataTestId(),
                element.cssClass(),
                String.join(" ", element.attributes().values())
        ));
        if (containsAny(text, "email")) {
            return "EMAIL";
        }
        if (containsAny(text, "password")) {
            return "PASSWORD";
        }
        if (containsAny(text, "search")) {
            return "SEARCH";
        }
        if (containsAny(text, "login", "sign in", "signin")) {
            return "LOGIN";
        }
        if (containsAny(text, "logout", "sign out", "signout")) {
            return "LOGOUT";
        }
        if ("HEADING".equals(technicalType)) {
            return containsAny(text, "dashboard") ? "DASHBOARD_HEADING" : "PAGE_HEADING";
        }
        if (containsAny(text, "userdropdown", "user dropdown", "profile menu", "user menu")) {
            return "USER_MENU_TRIGGER";
        }
        if (containsAny(text, "add to cart", "add-to-cart", "/cart/add", "buy")) {
            return "ADD_TO_CART";
        }
        if (containsAny(text, "remove", "delete", "cart/change")) {
            return "REMOVE";
        }
        if (containsAny(text, "/cart", "cart", "basket", "bag")) {
            return "CART";
        }
        if (containsAny(text, "details", "detail", "view", "record")) {
            return "DETAILS";
        }
        if (containsAny(text, "size")) {
            return "SIZE";
        }
        if (containsAny(text, "color", "colour")) {
            return "COLOR";
        }
        if (containsAny(text, "price")) {
            return "PRICE";
        }
        if (containsAny(text, "toast", "error", "success", "alert")) {
            return "STATUS_MESSAGE";
        }
        if ("TABLE".equals(technicalType)) {
            return "DATA_TABLE";
        }
        if ("MODAL".equals(technicalType)) {
            return "DIALOG";
        }
        return technicalType;
    }

    private List<String> actions(String technicalType, String semanticType, RawElement element) {
        if (!element.visible() || !element.enabled() || isHiddenElement(element)) {
            return List.of();
        }
        List<String> actions = new ArrayList<>();
        switch (technicalType) {
            case "EMAIL_INPUT", "PASSWORD_INPUT", "INPUT", "TEXTAREA" -> {
                actions.add("type");
                actions.add("clear");
            }
            case "BUTTON", "LINK" -> {
                if ("ADD_TO_CART".equals(semanticType)) {
                    actions.add("add-entity-to-container");
                } else if ("REMOVE".equals(semanticType)) {
                    actions.add("remove-entity-from-container");
                } else if ("CART".equals(semanticType)) {
                    actions.add("open-target-container");
                } else if ("DETAILS".equals(semanticType)) {
                    actions.add("open-details");
                } else if ("SEARCH".equals(semanticType)) {
                    actions.add("search");
                } else {
                    actions.add("click");
                }
            }
            case "CHECKBOX" -> {
                actions.add("check");
                actions.add("uncheck");
            }
            case "RADIO" -> actions.add("select");
            case "DROPDOWN" -> {
                actions.add("select-by-visible-text");
                actions.add("select-by-value");
            }
            case "FILE_INPUT" -> actions.add("upload");
            case "TABLE" -> {
                actions.add("read");
                actions.add("validate");
            }
            case "MODAL" -> {
                actions.add("close");
                actions.add("confirm");
                actions.add("cancel");
            }
            case "FORM" -> actions.add("submit");
            default -> {
                if ("SEARCH".equals(semanticType) && element.enabled()) {
                    actions.add("search");
                }
            }
        }
        return actions;
    }

    private boolean isHiddenElement(RawElement element) {
        return "input".equalsIgnoreCase(element.tag())
                && "hidden".equalsIgnoreCase(element.type());
    }

    private double confidence(String technicalType, String semanticType, RawElement element) {
        double score = "CONTENT".equals(technicalType) ? 0.55d : 0.82d;
        if (!element.dataTestId().isBlank() || !element.id().isBlank() || !element.ariaLabel().isBlank()) {
            score += 0.08d;
        }
        if (!semanticType.equals(technicalType)) {
            score += 0.05d;
        }
        return Math.min(1.0d, score);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "element";
    }

    private String sanitize(String value) {
        String normalized = normalize(value).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "element" : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... fragments) {
        String normalized = normalize(text);
        for (String fragment : fragments) {
            if (normalized.contains(normalize(fragment))) {
                return true;
            }
        }
        return false;
    }
}
