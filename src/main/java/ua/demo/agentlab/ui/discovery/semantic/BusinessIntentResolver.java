package ua.demo.agentlab.ui.discovery.semantic;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.semantic.model.ActionCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.BusinessIntentCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticElementModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BusinessIntentResolver {

    public List<BusinessIntentCandidate> resolveForElement(
            PageModel page,
            PageElementModel element,
            String semanticType,
            List<ActionCandidate> actions
    ) {
        if (ignoredSystemElement(element)) {
            return List.of();
        }
        String pageEvidence = pageEvidence(page);
        String elementEvidence = normalize(String.join(" ",
                semanticType,
                element.elementId(),
                element.name(),
                element.id(),
                element.placeholder(),
                element.ariaLabel(),
                element.role(),
                element.text(),
                element.href()
        ));
        String actionEvidence = normalize(actions.stream().map(ActionCandidate::action).toList().toString());
        Map<String, BusinessIntentCandidate> intents = new LinkedHashMap<>();

        boolean authPage = isAuthenticationEntryPage(pageEvidence, page);
        if (authPage && isCredentialFormElement(elementEvidence, actionEvidence)) {
            add(intents, "AUTHENTICATE", containsAny(actionEvidence, "submit_form", "click") ? 0.86d : 0.78d,
                    "business-context:credential-form", false);
        }
        if (containsAny(elementEvidence, "logout", "log out", "sign out") || containsAny(actionEvidence, "logout")) {
            add(intents, "LOGOUT", 0.92d, "business-context:logout-control", false);
        }
        if (containsAny(elementEvidence + " " + pageEvidence, "search")) {
            add(intents, "SEARCH", 0.88d, "business-context:search-control", false);
        }
        if (containsAny(elementEvidence + " " + actionEvidence, "filter")) {
            add(intents, "FILTER", 0.84d, "business-context:filter-control", false);
        }
        if (containsAny(elementEvidence + " " + actionEvidence, "sort_collection", "sort", "order")) {
            add(intents, "SORT_COLLECTION", 0.82d, "business-context:sort-control", false);
        }
        if (containsAny(elementEvidence + " " + actionEvidence, "paginate", "next", "previous")) {
            add(intents, "PAGINATE", 0.82d, "business-context:pagination-control", false);
        }
        if (containsAny(elementEvidence + " " + actionEvidence, "open_record", "details", "detail", "view", "profile")) {
            add(intents, "OPEN_RECORD", 0.82d, "business-context:open-record", false);
        }
        if (containsAny(elementEvidence + " " + actionEvidence, "create_record", "create", "add", "new")) {
            add(intents, "CREATE_RECORD", 0.82d, "business-context:create-record", false);
        }
        if (containsAny(elementEvidence + " " + actionEvidence, "edit_record", "edit", "update", "modify")) {
            add(intents, "EDIT_RECORD", 0.82d, "business-context:edit-record", false);
        }
        if (containsAny(elementEvidence + " " + actionEvidence, "delete_record", "delete", "remove")) {
            add(intents, "DELETE_RECORD", 0.82d, "business-context:delete-record", false);
        }
        if (containsAny(elementEvidence + " " + actionEvidence, "open_modal", "modal", "dialog", "popup")) {
            add(intents, "OPEN_MODAL", 0.82d, "business-context:open-modal", false);
        }
        if (containsAny(elementEvidence + " " + actionEvidence, "confirm_action", "confirm", "approve", "ok")) {
            add(intents, "CONFIRM_ACTION", 0.82d, "business-context:confirm-action", false);
        }
        if (containsAny(semanticType.toLowerCase(Locale.ROOT) + " " + pageEvidence, "collection", "table", "grid", "list")) {
            add(intents, "INSPECT_COLLECTION", 0.78d, "business-context:collection", false);
        }
        if (containsAny(actionEvidence, "click") && containsAny(semanticType.toLowerCase(Locale.ROOT), "link")) {
            add(intents, "NAVIGATE", 0.74d, "business-context:navigation-link", false);
        }
        if (intents.isEmpty() && !actions.isEmpty()) {
            add(intents, "INTERACT_WITH_ELEMENT", 0.55d, "business-context:needs-review", true);
        }
        return new ArrayList<>(intents.values());
    }

    public List<BusinessIntentCandidate> resolveForPage(PageModel page, List<SemanticElementModel> elements) {
        String evidence = pageEvidence(page) + " " + elements.stream()
                .flatMap(element -> element.businessIntentCandidates().stream())
                .map(BusinessIntentCandidate::intent)
                .toList();
        Map<String, BusinessIntentCandidate> intents = new LinkedHashMap<>();
        if (isAuthenticationEntryPage(pageEvidence(page), page)
                || elements.stream()
                .flatMap(element -> element.businessIntentCandidates().stream())
                .anyMatch(intent -> "AUTHENTICATE".equals(intent.intent()))) {
            add(intents, "AUTHENTICATION", 0.82d, "page-context:authentication", false);
        }
        if (containsAny(evidence, "authenticated", "dashboard", "protected")) {
            add(intents, "AUTHENTICATED_AREA", 0.80d, "page-context:authenticated-area", false);
        }
        if (containsAny(evidence, "inspect_collection", "table", "grid", "list")) {
            add(intents, "RECORD_LIST", 0.76d, "page-context:record-list", false);
        }
        if (intents.isEmpty()) {
            add(intents, "PAGE_CONTENT", 0.60d, "page-context:generic-content", true);
        }
        return new ArrayList<>(intents.values());
    }

    private boolean isAuthenticationEntryPage(String pageEvidence, PageModel page) {
        if (containsAny(pageEvidence, "authenticated-area", "authenticated area", "dashboard", "protected")) {
            return false;
        }
        return containsAny(pageEvidence, "login", "signin", "sign in", "auth/login", "username", "password")
                || hasCredentialPair(page);
    }

    private boolean isCredentialFormElement(String elementEvidence, String actionEvidence) {
        String evidence = elementEvidence + " " + actionEvidence;
        if (containsAny(evidence, "forgot", "footer", "orangehrm", "inc", "privacy", "external", "href")) {
            return false;
        }
        boolean credentialField = containsAny(evidence, "username", "user name", "userid", "user-id", "email", "password", "pass");
        boolean loginControl = containsAny(evidence, "login", "sign in", "signin", "submit_form")
                && !containsAny(evidence, "search", "filter", "lookup");
        boolean actionable = containsAny(actionEvidence, "type", "clear", "submit_form", "click");
        boolean formControl = containsAny(evidence, "input", "password_input", "button", "submit");
        return actionable && (credentialField || loginControl && formControl);
    }

    private boolean hasCredentialPair(PageModel page) {
        String text = page.elements().stream()
                .map(element -> String.join(" ", element.name(), element.id(), element.placeholder(), element.inputType(), element.semanticType()))
                .reduce("", (left, right) -> left + " " + right)
                .toLowerCase(Locale.ROOT);
        return containsAny(text, "username", "email") && text.contains("password");
    }

    private void add(
            Map<String, BusinessIntentCandidate> intents,
            String intent,
            double confidence,
            String evidence,
            boolean needsReview
    ) {
        BusinessIntentCandidate existing = intents.get(intent);
        if (existing == null || existing.confidence() < confidence) {
            intents.put(intent, new BusinessIntentCandidate(intent, confidence, List.of(evidence), needsReview));
        }
    }

    private String pageEvidence(PageModel page) {
        if (page == null) {
            return "";
        }
        return normalize(String.join(" ", page.pageId(), page.route(), page.title(), page.featureGuess(), page.visibleText()));
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
        return !element.visible()
                || "hidden".equalsIgnoreCase(element.inputType())
                || element.attributes().containsKey("hidden")
                || "true".equalsIgnoreCase(element.attributes().get("aria-hidden"))
                || containsAny(evidence, "_token", "csrf", "xsrf", "authenticity_token");
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
