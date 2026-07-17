package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.contract.UiOperationKind;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CanonicalOperationClassifier {

    private final CanonicalAliasDictionary aliasDictionary;

    public CanonicalOperationClassifier(CanonicalAliasDictionary aliasDictionary) {
        this.aliasDictionary = aliasDictionary;
    }

    public CanonicalOperationClassification classify(CanonicalInteractionEvidence evidence) {
        Map<UiOperationKind, Double> scores = new LinkedHashMap<>();
        String text = evidence.searchableText().toLowerCase(Locale.ROOT);

        score(scores, UiOperationKind.OPEN_PAGE, text, 0.25d, "open", "navigate", "visit", "go", "access");
        score(scores, UiOperationKind.VERIFY_PAGE_ACCESS, text, 0.30d, "public", "guest", "accessible", "reach");
        score(scores, UiOperationKind.INSPECT_PAGE_CONTENT, text, 0.28d, "content", "page", "section", "main", "render");
        score(scores, UiOperationKind.INSPECT_COLLECTION, text, 0.35d, "listing", "grid", "catalog", "results", "overview", "table", "collection");
        score(scores, UiOperationKind.INSPECT_ENTITY_SUMMARY, text, 0.34d, "card", "row", "record", "entry", "summary", "tile");
        score(scores, UiOperationKind.REVIEW_ENTITY_CONTENT, text, 0.34d, "information", "content", "field", "price", "name", "description");
        score(scores, UiOperationKind.OPEN_RECORD, text, 0.46d, "details", "detail", "view", "profile", "preview", "record");
        score(scores, UiOperationKind.CREATE_RECORD, text, 0.46d, "create", "add", "new record", "new item");
        score(scores, UiOperationKind.EDIT_RECORD, text, 0.46d, "edit", "update", "modify");
        score(scores, UiOperationKind.DELETE_RECORD, text, 0.46d, "delete", "remove");
        score(scores, UiOperationKind.OPEN_MODAL, text, 0.44d, "modal", "dialog", "popup");
        score(scores, UiOperationKind.CONFIRM_ACTION, text, 0.44d, "confirm", "approve", "ok", "yes");
        score(scores, UiOperationKind.SEARCH, text, 0.45d, "search", "find", "lookup", "query");
        score(scores, UiOperationKind.FILTER, text, 0.42d, "filter", "refine");
        score(scores, UiOperationKind.SORT_COLLECTION, text, 0.42d, "sort", "order");
        score(scores, UiOperationKind.PAGINATE, text, 0.42d, "paginate", "next page", "previous page", "page next", "page previous");
        score(scores, UiOperationKind.AUTHENTICATE, text, 0.48d, "login", "signin", "auth", "password", "username");
        score(scores, UiOperationKind.SUBMIT_FORM, text, 0.44d, "submit", "save", "create", "update", "send");
        score(scores, UiOperationKind.UPLOAD_FILE, text, 0.50d, "upload", "attach", "import");
        score(scores, UiOperationKind.DOWNLOAD_FILE, text, 0.50d, "download", "export");
        score(scores, UiOperationKind.LOGOUT, text, 0.50d, "logout", "signout");
        score(scores, UiOperationKind.HTTP_AUTHENTICATE, text, 0.60d, "http auth", "basic auth", "digest auth");
        score(scores, UiOperationKind.ACCEPT_ALERT, text, 0.56d, "accept alert", "accept javascript alert");
        score(scores, UiOperationKind.DISMISS_ALERT, text, 0.56d, "dismiss alert", "cancel alert");
        score(scores, UiOperationKind.ENTER_ALERT_TEXT, text, 0.58d, "alert text", "prompt text", "javascript prompt");
        score(scores, UiOperationKind.OPEN_NEW_WINDOW, text, 0.56d, "new window", "open window", "new tab");
        score(scores, UiOperationKind.SWITCH_WINDOW, text, 0.56d, "switch window", "switch tab");
        score(scores, UiOperationKind.HOVER, text, 0.56d, "hover", "mouse over", "pointer over");
        score(scores, UiOperationKind.SET_SLIDER, text, 0.58d, "slider", "range input", "set range");

        if (aliasDictionary.matchesContainer(evidence)) {
            score(scores, UiOperationKind.OPEN_TARGET_CONTAINER, 0.25d);
        }
        if (aliasDictionary.matchesContainer(evidence) && containsAny(text, "add", "save", "include")) {
            score(scores, UiOperationKind.ADD_ENTITY_TO_CONTAINER, 0.70d);
        }
        if (aliasDictionary.matchesContainer(evidence) && containsAny(text, "remove", "delete", "clear")) {
            score(scores, UiOperationKind.REMOVE_ENTITY_FROM_CONTAINER, 0.70d);
        }
        if (aliasDictionary.matchesDetails(evidence)) {
            score(scores, UiOperationKind.OPEN_RECORD, 0.20d);
        }
        if (aliasDictionary.matchesCollection(evidence)) {
            score(scores, UiOperationKind.INSPECT_COLLECTION, 0.12d);
        }
        if (aliasDictionary.matchesEntity(evidence)) {
            score(scores, UiOperationKind.INSPECT_ENTITY_SUMMARY, 0.12d);
            score(scores, UiOperationKind.REVIEW_ENTITY_CONTENT, 0.10d);
        }
        if (aliasDictionary.matchesAuth(evidence)) {
            score(scores, UiOperationKind.AUTHENTICATE, 0.18d);
        }
        if (aliasDictionary.matchesForm(evidence)) {
            score(scores, UiOperationKind.SUBMIT_FORM, 0.14d);
        }
        if (aliasDictionary.matchesFile(evidence)) {
            score(scores, UiOperationKind.UPLOAD_FILE, containsAny(text, "upload", "attach", "import") ? 0.22d : 0.0d);
            score(scores, UiOperationKind.DOWNLOAD_FILE, containsAny(text, "download", "export") ? 0.22d : 0.0d);
        }

        for (String route : evidence.extractedRoutes()) {
            String normalized = route.toLowerCase(Locale.ROOT);
            if (normalized.contains("/search") || normalized.contains("/results")) {
                score(scores, UiOperationKind.SEARCH, 0.18d);
            }
            if (normalized.contains("/login") || normalized.contains("/auth")) {
                score(scores, UiOperationKind.AUTHENTICATE, 0.18d);
            }
            if (normalized.contains("/details") || normalized.contains("/item") || normalized.contains("/product")) {
                score(scores, UiOperationKind.OPEN_RECORD, 0.18d);
            }
            if (normalized.contains("/cart") || normalized.contains("/basket") || normalized.contains("/wishlist")) {
                score(scores, UiOperationKind.OPEN_TARGET_CONTAINER, 0.18d);
            }
        }

        if (scores.isEmpty()) {
            score(scores, UiOperationKind.OPEN_PAGE, 0.20d);
        }

        Map.Entry<UiOperationKind, Double> winner = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();

        List<String> domainHints = new ArrayList<>(aliasDictionary.resolveDomainHints(evidence));
        return new CanonicalOperationClassification(
                winner.getKey(),
                canonicalName(winner.getKey()),
                "Resolved from evidence scoring across action, element, routes, and aliases",
                Math.min(1.0d, Math.max(0.45d, winner.getValue())),
                List.copyOf(domainHints)
        );
    }

    private void score(Map<UiOperationKind, Double> scores, UiOperationKind kind, double amount) {
        if (amount <= 0.0d) {
            return;
        }
        scores.merge(kind, amount, Double::sum);
    }

    private void score(
            Map<UiOperationKind, Double> scores,
            UiOperationKind kind,
            String text,
            double amount,
            String... terms
    ) {
        for (String term : terms) {
            if (containsAny(text, term)) {
                score(scores, kind, amount);
            }
        }
    }

    private boolean containsAny(String text, String... terms) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (normalized.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String canonicalName(UiOperationKind kind) {
        return switch (kind) {
            case OPEN_PAGE -> "openPage";
            case VERIFY_PAGE_ACCESS -> "verifyPageAccess";
            case INSPECT_PAGE_CONTENT -> "inspectPageContent";
            case INSPECT_COLLECTION -> "inspectCollection";
            case INSPECT_ENTITY_SUMMARY -> "inspectEntitySummary";
            case REVIEW_ENTITY_CONTENT, REVIEW_ITEM_CONTENT -> "reviewEntityContent";
            case OPEN_RECORD, OPEN_DETAILS -> "openRecord";
            case CREATE_RECORD -> "createRecord";
            case EDIT_RECORD -> "editRecord";
            case DELETE_RECORD -> "deleteRecord";
            case OPEN_MODAL -> "openModal";
            case CONFIRM_ACTION -> "confirmAction";
            case OPEN_TARGET_CONTAINER, OPEN_DESTINATION_CONTAINER -> "openTargetContainer";
            case ADD_ENTITY_TO_CONTAINER, ADD_ITEM_TO_CONTAINER -> "addEntityToContainer";
            case REMOVE_ENTITY_FROM_CONTAINER, REMOVE_ITEM_FROM_CONTAINER -> "removeEntityFromContainer";
            case AUTHENTICATE -> "authenticate";
            case ENTER_TEXT -> "enterText";
            case SUBMIT_FORM -> "submitForm";
            case SEARCH -> "search";
            case FILTER -> "applyFilter";
            case SORT_COLLECTION, SORT -> "sortCollection";
            case PAGINATE -> "paginate";
            case LOGOUT -> "logout";
            case UPLOAD_FILE -> "uploadFile";
            case DOWNLOAD_FILE -> "downloadFile";
            case HTTP_AUTHENTICATE -> "authenticateHttpChallenge";
            case ACCEPT_ALERT -> "acceptAlert";
            case DISMISS_ALERT -> "dismissAlert";
            case ENTER_ALERT_TEXT -> "enterAlertText";
            case OPEN_NEW_WINDOW -> "openNewWindow";
            case SWITCH_WINDOW -> "switchWindow";
            case HOVER -> "hover";
            case SET_SLIDER -> "setSlider";
            case VERIFY_PUBLIC_ACCESS -> "verifyPublicAccess";
            case INSPECT_LISTING -> "inspectCollection";
            case INSPECT_ITEM_CARDS -> "inspectEntitySummary";
        };
    }

    public record CanonicalOperationClassification(
            UiOperationKind operationKind,
            String canonicalName,
            String rationale,
            double confidence,
            List<String> domainHints
    ) {
    }
}
