package ua.demo.agentlab.ui.discovery.semantic;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageActionModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.interaction.compatibility.ActionCompatibilityService;
import ua.demo.agentlab.ui.discovery.semantic.model.ActionCandidate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActionCandidateClassifier {

    private final ActionCompatibilityService compatibilityService;

    public ActionCandidateClassifier() {
        this(new ActionCompatibilityService());
    }

    ActionCandidateClassifier(ActionCompatibilityService compatibilityService) {
        this.compatibilityService = compatibilityService == null
                ? new ActionCompatibilityService()
                : compatibilityService;
    }

    public List<ActionCandidate> classify(PageElementModel element, String semanticType) {
        if (element == null || !element.visible() || ignoredSystemElement(element)) {
            return List.of();
        }
        Map<String, ActionCandidate> candidates = new LinkedHashMap<>();
        for (PageActionModel action : element.actions()) {
            add(candidates, normalizeAction(action.actionType()), element.elementId(), action.confidenceScore(),
                    "page-model-action:" + action.actionId());
        }

        String evidence = normalize(String.join(" ",
                semanticType,
                element.technicalType(),
                element.semanticType(),
                element.tag(),
                element.inputType(),
                element.role(),
                element.text(),
                element.name(),
                element.id(),
                element.placeholder(),
                element.cssClass()
        ));
        switch (semanticType == null ? "" : semanticType.toUpperCase(Locale.ROOT)) {
            case "BUTTON", "LINK" -> add(candidates, "CLICK", element.elementId(), 0.88d, "semantic-element:" + semanticType);
            case "INPUT", "PASSWORD_INPUT", "TEXTAREA" -> {
                add(candidates, "TYPE", element.elementId(), 0.90d, "semantic-element:" + semanticType);
                add(candidates, "CLEAR", element.elementId(), 0.78d, "semantic-element:" + semanticType);
            }
            case "SELECT" -> add(candidates, "SELECT", element.elementId(), 0.90d, "semantic-element:SELECT");
            case "CHECKBOX" -> {
                add(candidates, "CHECK", element.elementId(), 0.86d, "semantic-element:CHECKBOX");
                add(candidates, "UNCHECK", element.elementId(), 0.86d, "semantic-element:CHECKBOX");
            }
            case "FILE_INPUT" -> add(candidates, "UPLOAD", element.elementId(), 0.90d, "semantic-element:FILE_INPUT");
            case "RANGE_SLIDER" -> add(candidates, "SET_SLIDER", element.elementId(), 0.90d,
                    "semantic-element:RANGE_SLIDER");
            case "IMAGE" -> add(candidates, "HOVER", element.elementId(), 0.78d, "semantic-element:IMAGE");
            case "COLLECTION" -> add(candidates, "READ", element.elementId(), 0.78d, "semantic-element:COLLECTION");
            default -> {
                if (containsAny(evidence, "button", "click", "submit", "link")) {
                    add(candidates, "CLICK", element.elementId(), 0.72d, "semantic-evidence:clickable");
                }
            }
        }
        if (containsAny(evidence, "submit", "login", "sign in", "save", "create", "send")) {
            add(candidates, "SUBMIT_FORM", element.elementId(), 0.82d, "semantic-evidence:submit");
        }
        if (containsAny(evidence, "search")) {
            add(candidates, "SEARCH", element.elementId(), 0.84d, "semantic-evidence:search");
        }
        if (containsAny(evidence, "filter")) {
            add(candidates, "FILTER", element.elementId(), 0.82d, "semantic-evidence:filter");
        }
        if (containsAny(evidence, "sort", "order")) {
            add(candidates, "SORT_COLLECTION", element.elementId(), 0.82d, "semantic-evidence:sort");
        }
        if (containsAny(evidence, "next", "previous", "pagination", "page")) {
            add(candidates, "PAGINATE", element.elementId(), 0.80d, "semantic-evidence:pagination");
        }
        if (containsAny(evidence, "view", "details", "detail", "profile", "record")) {
            add(candidates, "OPEN_RECORD", element.elementId(), 0.80d, "semantic-evidence:open-record");
        }
        if (containsAny(evidence, "create", "add", "new")) {
            add(candidates, "CREATE_RECORD", element.elementId(), 0.80d, "semantic-evidence:create-record");
        }
        if (containsAny(evidence, "edit", "update", "modify")) {
            add(candidates, "EDIT_RECORD", element.elementId(), 0.80d, "semantic-evidence:edit-record");
        }
        if (containsAny(evidence, "delete", "remove")) {
            add(candidates, "DELETE_RECORD", element.elementId(), 0.80d, "semantic-evidence:delete-record");
        }
        if (containsAny(evidence, "modal", "dialog", "popup")) {
            add(candidates, "OPEN_MODAL", element.elementId(), 0.80d, "semantic-evidence:open-modal");
        }
        if (containsAny(evidence, "confirm", "approve", "ok")) {
            add(candidates, "CONFIRM_ACTION", element.elementId(), 0.80d, "semantic-evidence:confirm");
        }
        if (containsAny(evidence, "logout", "log out", "sign out")) {
            add(candidates, "LOGOUT", element.elementId(), 0.92d, "semantic-evidence:logout");
        }
        if (containsAny(evidence, "new window", "open window")) {
            add(candidates, "OPEN_NEW_WINDOW", element.elementId(), 0.86d, "semantic-evidence:new-window");
        }
        if (containsAny(evidence, "user_menu_trigger", "user menu trigger", "userdropdown-tab", "aria-haspopup")) {
            add(candidates, "OPEN_MENU", element.elementId(), 0.86d, "semantic-evidence:user-menu");
        }
        return candidates.values().stream()
                .filter(candidate -> compatibilityService.allows(element, semanticType, candidate.action()))
                .toList();
    }

    private void add(
            Map<String, ActionCandidate> candidates,
            String action,
            String elementId,
            double confidence,
            String evidence
    ) {
        if (action == null || action.isBlank()) {
            return;
        }
        String key = action.toUpperCase(Locale.ROOT);
        ActionCandidate existing = candidates.get(key);
        if (existing == null || existing.confidence() < confidence) {
            candidates.put(key, new ActionCandidate(key, elementId, confidence, List.of(evidence)));
        }
    }

    private String normalizeAction(String actionType) {
        String normalized = normalize(actionType);
        return switch (normalized) {
            case "type", "typetext", "input" -> "TYPE";
            case "clear" -> "CLEAR";
            case "click", "open" -> "CLICK";
            case "details", "detail", "view", "openrecord" -> "OPEN_RECORD";
            case "create", "add", "new", "createrecord" -> "CREATE_RECORD";
            case "edit", "update", "modify", "editrecord" -> "EDIT_RECORD";
            case "delete", "remove", "deleterecord" -> "DELETE_RECORD";
            case "submit", "submitform" -> "SUBMIT_FORM";
            case "select" -> "SELECT";
            case "filter" -> "FILTER";
            case "sort", "order", "sortcollection" -> "SORT_COLLECTION";
            case "paginate", "next", "previous" -> "PAGINATE";
            case "openmodal", "modal", "dialog" -> "OPEN_MODAL";
            case "openmenu", "menu", "dropdown" -> "OPEN_MENU";
            case "confirm", "approve", "confirmaction" -> "CONFIRM_ACTION";
            case "check" -> "CHECK";
            case "uncheck" -> "UNCHECK";
            case "upload" -> "UPLOAD";
            case "hover" -> "HOVER";
            case "setslider", "slider", "range" -> "SET_SLIDER";
            case "opennewwindow", "openwindow" -> "OPEN_NEW_WINDOW";
            case "download" -> "DOWNLOAD";
            case "read", "inspect" -> "READ";
            default -> actionType == null ? "" : actionType.trim().toUpperCase(Locale.ROOT);
        };
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
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean ignoredSystemElement(PageElementModel element) {
        String evidence = normalize(String.join(" ",
                element.inputType(),
                element.name(),
                element.id(),
                element.semanticType(),
                element.technicalType(),
                element.attributes().toString()
        ));
        return "hidden".equalsIgnoreCase(element.inputType())
                || element.attributes().containsKey("hidden")
                || "true".equalsIgnoreCase(element.attributes().get("aria-hidden"))
                || containsAny(evidence, "_token", "csrf", "xsrf", "authenticity_token");
    }
}
