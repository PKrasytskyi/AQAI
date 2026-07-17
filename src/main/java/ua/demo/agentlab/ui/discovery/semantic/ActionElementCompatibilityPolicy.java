package ua.demo.agentlab.ui.discovery.semantic;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;

import java.util.Locale;
import java.util.Set;

/** Rejects semantic actions that cannot be performed by the target element role. */
public final class ActionElementCompatibilityPolicy {

    private static final Set<String> CLICKABLE_ACTIONS = Set.of(
            "CLICK", "SUBMIT_FORM", "SEARCH", "FILTER", "SORT_COLLECTION", "PAGINATE",
            "OPEN_RECORD", "CREATE_RECORD", "EDIT_RECORD", "DELETE_RECORD", "OPEN_MODAL",
            "OPEN_MENU", "CONFIRM_ACTION", "LOGOUT", "DOWNLOAD", "OPEN_NEW_WINDOW"
    );

    public boolean allows(PageElementModel element, String semanticType, String action) {
        if (element == null || action == null || action.isBlank()) {
            return false;
        }
        String type = normalize(firstNonBlank(semanticType, element.semanticType(), element.technicalType()));
        String intent = normalize(action);
        String tag = normalize(element.tag());
        String inputType = normalize(element.inputType());

        if ("READ".equals(intent)) {
            return true;
        }
        if (Set.of("TYPE", "CLEAR").contains(intent)) {
            return Set.of("INPUT", "PASSWORD_INPUT", "TEXTAREA").contains(type)
                    || Set.of("INPUT", "TEXTAREA").contains(tag) && !"HIDDEN".equals(inputType);
        }
        if ("SELECT".equals(intent)) {
            return "SELECT".equals(type) || "SELECT".equals(tag);
        }
        if (Set.of("CHECK", "UNCHECK").contains(intent)) {
            return "CHECKBOX".equals(type) || "CHECKBOX".equals(inputType);
        }
        if ("UPLOAD".equals(intent)) {
            return "FILE_INPUT".equals(type) || "FILE".equals(inputType);
        }
        if ("SET_SLIDER".equals(intent)) {
            return "RANGE_SLIDER".equals(type) || "RANGE".equals(inputType);
        }
        if ("HOVER".equals(intent)) {
            return Set.of("IMAGE", "LINK", "BUTTON").contains(type)
                    || Set.of("IMG", "A", "BUTTON").contains(tag);
        }
        if (CLICKABLE_ACTIONS.contains(intent)) {
            boolean clickable = Set.of("BUTTON", "LINK").contains(type)
                    || Set.of("BUTTON", "A").contains(tag)
                    || "BUTTON".equals(normalize(element.role()))
                    || "SUBMIT".equals(inputType);
            if (!clickable) {
                return false;
            }
            return !"SUBMIT_FORM".equals(intent)
                    || "SUBMIT".equals(inputType)
                    || "BUTTON".equals(tag)
                    || "FORM".equals(type);
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
