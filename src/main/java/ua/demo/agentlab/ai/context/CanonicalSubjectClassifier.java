package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.context.CanonicalOperationClassifier.CanonicalOperationClassification;
import ua.demo.agentlab.ui.contract.UiOperationKind;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CanonicalSubjectClassifier {

    private final CanonicalAliasDictionary aliasDictionary;

    public CanonicalSubjectClassifier(CanonicalAliasDictionary aliasDictionary) {
        this.aliasDictionary = aliasDictionary;
    }

    public CanonicalSubjectClassification classify(
            CanonicalInteractionEvidence evidence,
            CanonicalOperationClassification operation
    ) {
        String subjectType = switch (operation.operationKind()) {
            case OPEN_PAGE, VERIFY_PAGE_ACCESS, VERIFY_PUBLIC_ACCESS -> "page";
            case INSPECT_COLLECTION, INSPECT_LISTING, SEARCH, FILTER, SORT, SORT_COLLECTION, PAGINATE -> "collection";
            case INSPECT_PAGE_CONTENT -> "page";
            case INSPECT_ENTITY_SUMMARY, INSPECT_ITEM_CARDS, REVIEW_ENTITY_CONTENT, REVIEW_ITEM_CONTENT,
                    OPEN_DETAILS, OPEN_RECORD, CREATE_RECORD, EDIT_RECORD, DELETE_RECORD -> "entity";
            case OPEN_TARGET_CONTAINER, OPEN_DESTINATION_CONTAINER, ADD_ENTITY_TO_CONTAINER, ADD_ITEM_TO_CONTAINER,
                    REMOVE_ENTITY_FROM_CONTAINER, REMOVE_ITEM_FROM_CONTAINER -> "container";
            case AUTHENTICATE, LOGOUT -> "session";
            case SUBMIT_FORM -> "form";
            case OPEN_MODAL, CONFIRM_ACTION -> "interaction";
            case UPLOAD_FILE, DOWNLOAD_FILE -> "file";
        };

        String targetType = switch (operation.operationKind()) {
            case OPEN_PAGE, VERIFY_PAGE_ACCESS, VERIFY_PUBLIC_ACCESS -> "page-access";
            case INSPECT_COLLECTION, INSPECT_LISTING -> "collection";
            case INSPECT_ENTITY_SUMMARY, INSPECT_ITEM_CARDS -> "entity-summary";
            case INSPECT_PAGE_CONTENT, REVIEW_ENTITY_CONTENT, REVIEW_ITEM_CONTENT -> "content";
            case OPEN_DETAILS, OPEN_RECORD -> "record-details";
            case CREATE_RECORD -> "created-record";
            case EDIT_RECORD -> "edited-record";
            case DELETE_RECORD -> "deleted-record";
            case SEARCH -> "search-results";
            case FILTER, SORT, SORT_COLLECTION -> "refined-results";
            case PAGINATE -> "paged-results";
            case OPEN_TARGET_CONTAINER, OPEN_DESTINATION_CONTAINER,
                    ADD_ENTITY_TO_CONTAINER, ADD_ITEM_TO_CONTAINER,
                    REMOVE_ENTITY_FROM_CONTAINER, REMOVE_ITEM_FROM_CONTAINER -> "container";
            case AUTHENTICATE -> "authenticated-session";
            case SUBMIT_FORM -> "form-submission";
            case OPEN_MODAL -> "modal";
            case CONFIRM_ACTION -> "confirmed-action";
            case UPLOAD_FILE -> "uploaded-file";
            case DOWNLOAD_FILE -> "downloaded-file";
            case LOGOUT -> "signed-out-session";
        };

        Set<String> domainHints = new LinkedHashSet<>(operation.domainHints());
        domainHints.addAll(aliasDictionary.resolveDomainHints(evidence));
        String targetRoute = evidence.extractedRoutes().stream().findFirst().orElse(firstNonBlank(evidence.pageUrlPattern(), evidence.pageUrl()));

        return new CanonicalSubjectClassification(
                subjectType,
                targetType,
                List.copyOf(domainHints),
                targetRoute == null ? "" : targetRoute
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public record CanonicalSubjectClassification(
            String subjectType,
            String targetType,
            List<String> domainHints,
            String targetRoute
    ) {
    }
}
