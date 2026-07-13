package ua.demo.agentlab.artifactreuse.flow;

import ua.demo.agentlab.ui.contract.UiOperationKind;

public enum FlowActionType {
    OPEN_PAGE,
    NAVIGATE,
    CLICK,
    ENTER_TEXT,
    CLEAR_FIELD,
    SELECT_OPTION,
    TOGGLE_CONTROL,
    SUBMIT_FORM,
    AUTHENTICATE,
    LOGOUT,
    SEARCH,
    FILTER,
    SORT,
    PAGINATE,
    OPEN_RECORD,
    CREATE_RECORD,
    EDIT_RECORD,
    DELETE_RECORD,
    OPEN_MODAL,
    OPEN_MENU,
    CONFIRM_ACTION,
    UPLOAD_FILE,
    DOWNLOAD_FILE,
    INSPECT_COLLECTION,
    MUTATE_CONTAINER,
    ASSERT;

    public static FlowActionType from(UiOperationKind kind) {
        if (kind == null) {
            return CLICK;
        }
        return switch (kind) {
            case OPEN_PAGE -> OPEN_PAGE;
            case VERIFY_PAGE_ACCESS, VERIFY_PUBLIC_ACCESS -> ASSERT;
            case INSPECT_PAGE_CONTENT, REVIEW_ENTITY_CONTENT, REVIEW_ITEM_CONTENT -> ASSERT;
            case INSPECT_COLLECTION, INSPECT_LISTING, INSPECT_ITEM_CARDS, INSPECT_ENTITY_SUMMARY -> INSPECT_COLLECTION;
            case OPEN_TARGET_CONTAINER, OPEN_DESTINATION_CONTAINER -> NAVIGATE;
            case ADD_ENTITY_TO_CONTAINER, REMOVE_ENTITY_FROM_CONTAINER, ADD_ITEM_TO_CONTAINER, REMOVE_ITEM_FROM_CONTAINER -> MUTATE_CONTAINER;
            case OPEN_DETAILS, OPEN_RECORD -> OPEN_RECORD;
            case AUTHENTICATE -> AUTHENTICATE;
            case ENTER_TEXT -> ENTER_TEXT;
            case SUBMIT_FORM -> SUBMIT_FORM;
            case SEARCH -> SEARCH;
            case FILTER -> FILTER;
            case CREATE_RECORD -> CREATE_RECORD;
            case EDIT_RECORD -> EDIT_RECORD;
            case DELETE_RECORD -> DELETE_RECORD;
            case OPEN_MODAL -> OPEN_MODAL;
            case CONFIRM_ACTION -> CONFIRM_ACTION;
            case SORT_COLLECTION, SORT -> SORT;
            case PAGINATE -> PAGINATE;
            case LOGOUT -> LOGOUT;
            case UPLOAD_FILE -> UPLOAD_FILE;
            case DOWNLOAD_FILE -> DOWNLOAD_FILE;
        };
    }
}
