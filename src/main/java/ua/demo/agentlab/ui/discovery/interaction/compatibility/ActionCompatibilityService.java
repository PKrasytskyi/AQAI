package ua.demo.agentlab.ui.discovery.interaction.compatibility;

import ua.demo.agentlab.ui.discovery.interaction.model.SemanticAction;
import ua.demo.agentlab.ui.discovery.interaction.raw.RawUiElementEvidence;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** One technical and semantic compatibility policy for every mapper mode. */
public final class ActionCompatibilityService {

    private static final Set<String> CLICKABLE_ACTIONS = Set.of(
            "CLICK", "SUBMIT_FORM", "SEARCH", "FILTER", "SORT_COLLECTION", "PAGINATE",
            "OPEN_RECORD", "CREATE_RECORD", "EDIT_RECORD", "DELETE_RECORD", "OPEN_MODAL",
            "OPEN_MENU", "CONFIRM_ACTION", "LOGOUT", "DOWNLOAD", "OPEN_NEW_WINDOW"
    );

    public List<ActionCompatibilityResult> compatibleActions(RawUiElementEvidence element) {
        if (element == null || !element.visible() || !element.enabled() || isSystemElement(element)) return List.of();
        Map<SemanticAction, ActionCompatibilityResult> result = new LinkedHashMap<>();
        String tag = normalize(element.tag());
        String type = normalize(element.inputType());
        String role = normalize(element.role());
        String semantics = normalize(String.join(" ", element.text(), element.ariaLabel(), element.placeholder(),
                element.idAttribute(), element.nameAttribute(), element.cssClass(), element.attributes().toString()));

        if (tag.equals("input") && !type.equals("hidden") || tag.equals("textarea")) {
            if (type.equals("checkbox")) {
                add(result, SemanticAction.CHECK, 0.95d, "checkbox input");
                add(result, SemanticAction.UNCHECK, 0.95d, "checkbox input");
            } else if (type.equals("file")) {
                add(result, SemanticAction.UPLOAD, 0.98d, "file input");
            } else if (type.equals("range")) {
                add(result, SemanticAction.SET_SLIDER, 0.98d, "range input");
            } else if (!type.equals("submit") && !type.equals("button")) {
                add(result, SemanticAction.TYPE, 0.96d, "editable input");
                add(result, SemanticAction.CLEAR, 0.88d, "editable input");
            }
        }
        if (tag.equals("select") || role.equals("combobox")) add(result, SemanticAction.SELECT, 0.97d, "select/combobox");
        boolean clickable = tag.equals("button") || tag.equals("a") || role.equals("button")
                || type.equals("submit") || type.equals("button");
        if (clickable) add(result, SemanticAction.CLICK, 0.92d, "clickable element");
        if (type.equals("submit") || tag.equals("button") && containsAny(semantics, "submit", "login", "sign in")) {
            add(result, SemanticAction.SUBMIT_FORM, 0.94d, "form submit control");
        }
        if (tag.equals("a") && !element.href().isBlank()) add(result, SemanticAction.NAVIGATE, 0.93d, "link with href");
        if (clickable && containsAny(semantics, "search", "find")) add(result, SemanticAction.SEARCH, 0.90d, "search semantics");
        if (clickable && containsAny(semantics, "filter", "apply")) add(result, SemanticAction.FILTER, 0.86d, "filter semantics");
        if (clickable && containsAny(semantics, "menu", "dropdown", "haspopup", "userdropdown")) {
            add(result, SemanticAction.OPEN_MENU, 0.90d, "menu trigger semantics");
        }
        if (clickable && containsAny(semantics, "logout", "log out", "sign out")) {
            add(result, SemanticAction.LOGOUT, 0.96d, "logout semantics");
        }
        return List.copyOf(result.values());
    }

    public ActionCompatibilityResult evaluate(RawUiElementEvidence element, SemanticAction action) {
        return compatibleActions(element).stream().filter(item -> item.action() == action).findFirst()
                .orElse(new ActionCompatibilityResult(action, false, 0.0d, List.of("element/action types are incompatible")));
    }

    public boolean allows(PageElementModel element, String semanticType, String action) {
        if (element == null || action == null || action.isBlank()) return false;
        String type = canonical(firstNonBlank(semanticType, element.semanticType(), element.technicalType()));
        String intent = canonical(action);
        String tag = canonical(element.tag());
        String inputType = canonical(element.inputType());
        if ("READ".equals(intent)) return true;
        if (Set.of("TYPE", "CLEAR").contains(intent)) {
            return Set.of("INPUT", "PASSWORD_INPUT", "TEXTAREA").contains(type)
                    || Set.of("INPUT", "TEXTAREA").contains(tag) && !"HIDDEN".equals(inputType);
        }
        if ("SELECT".equals(intent)) return "SELECT".equals(type) || "SELECT".equals(tag);
        if (Set.of("CHECK", "UNCHECK").contains(intent)) {
            return "CHECKBOX".equals(type) || "CHECKBOX".equals(inputType);
        }
        if ("UPLOAD".equals(intent)) return "FILE_INPUT".equals(type) || "FILE".equals(inputType);
        if ("SET_SLIDER".equals(intent)) return "RANGE_SLIDER".equals(type) || "RANGE".equals(inputType);
        if ("HOVER".equals(intent)) {
            return Set.of("IMAGE", "LINK", "BUTTON").contains(type)
                    || Set.of("IMG", "A", "BUTTON").contains(tag);
        }
        if ("OPEN_MENU".equals(intent)) return isSemanticMenuTrigger(element);
        if (CLICKABLE_ACTIONS.contains(intent)) {
            boolean clickable = Set.of("BUTTON", "LINK").contains(type)
                    || Set.of("BUTTON", "A").contains(tag)
                    || "BUTTON".equals(canonical(element.role()))
                    || "SUBMIT".equals(inputType);
            return clickable && (!"SUBMIT_FORM".equals(intent)
                    || "SUBMIT".equals(inputType) || "BUTTON".equals(tag) || "FORM".equals(type));
        }
        return false;
    }

    private void add(Map<SemanticAction, ActionCompatibilityResult> target, SemanticAction action,
                     double confidence, String reason) {
        target.merge(action, new ActionCompatibilityResult(action, true, confidence, List.of(reason)),
                (left, right) -> right.confidence() > left.confidence() ? right : left);
    }

    private boolean isSystemElement(RawUiElementEvidence element) {
        String evidence = normalize(element.inputType() + " " + element.nameAttribute() + " "
                + element.idAttribute() + " " + element.attributes());
        return evidence.contains("hidden") || containsAny(evidence, "_token", "csrf", "xsrf", "authenticity token");
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) if (value.contains(fragment)) return true;
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isSemanticMenuTrigger(PageElementModel element) {
        String evidence = canonical(String.join(" ", element.elementId(), element.name(), element.id(),
                element.cssClass(), element.ariaLabel(), element.role(), element.attributes().toString()));
        return evidence.contains("MENU_TRIGGER")
                || evidence.contains("USERDROPDOWN_TAB")
                || evidence.contains("ARIA_HASPOPUP");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private String canonical(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
