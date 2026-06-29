package ua.demo.agentlab.templates.assertions;

import ua.demo.agentlab.templates.ui.UiOperationType;
import ua.demo.agentlab.templates.ui.UiScenarioTemplateContext;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AssertionTemplateLibrary {

    public String buildAssertionBlock(UiOperationType operationType, UiScenarioTemplateContext context) {
        Objects.requireNonNull(operationType, "operationType must not be null");
        Objects.requireNonNull(context, "context must not be null");

        String intentDrivenBlock = buildIntentDrivenAssertionBlock(context);
        if (!intentDrivenBlock.isBlank()) {
            return intentDrivenBlock;
        }

        String scenarioDrivenBlock = buildScenarioDrivenAssertionBlock(context);
        if (!scenarioDrivenBlock.isBlank()) {
            return scenarioDrivenBlock;
        }

        return switch (operationType) {
            case PAGE_OPEN -> positiveStateAssertion(context, "Expected target page to be opened successfully");
            case AUTH_FLOW -> positiveStateAssertion(
                    context,
                    "Expected authentication to succeed"
            ) + negativeStateAbsenceAssertion(
                    context,
                    "Did not expect authentication error state after valid sign in"
            );
            case AUTH_FLOW_NEGATIVE -> negativeStateAssertion(
                    context,
                    "Expected authentication to fail for invalid or restricted credentials"
            );
            case FORM_SUBMIT, ENTITY_CREATE, ENTITY_UPDATE -> positiveStateAssertion(
                    context,
                    "Expected form submission to succeed"
            );
            case FORM_SUBMIT_NEGATIVE -> negativeStateAssertion(
                    context,
                    "Expected validation or error state after invalid form submission"
            );
            case FLOW_NAVIGATION -> positiveStateAssertion(context, "Expected navigation target to be opened");
            case DATA_SEARCH, DATA_FILTER -> """
                    UiAssertions.assertFalse(
                            %s.%s(),
                            "Expected results to be present"
                    );
                    """.formatted(
                    context.assertionPageVariableName(),
                    context.emptyStateMethodName()
            ) + optionalUrlAssertion(context);
            case DETAILS_OPEN -> """
                    UiAssertions.assertTrue(
                            %s.%s(),
                            "Expected details view to be visible"
                    );
                    """.formatted(
                    context.assertionPageVariableName(),
                    context.detailsStateMethodName()
            ) + optionalUrlAssertion(context);
            case ENTITY_DELETE -> positiveStateAssertion(context, "Expected entity to be deleted successfully");
            case SESSION_END -> positiveStateAssertion(context, "Expected logout flow to complete successfully");
            case FILE_UPLOAD -> positiveStateAssertion(context, "Expected file upload to complete successfully");
            case FILE_DOWNLOAD -> positiveStateAssertion(
                    context,
                    "Expected file download to be triggered successfully"
            );
            case SECURITY_STEP -> positiveStateAssertion(
                    context,
                    "Expected security challenge or follow-up step to appear"
            );
        };
    }

    private String buildIntentDrivenAssertionBlock(UiScenarioTemplateContext context) {
        if (context.assertionIntents().isEmpty()) {
            return "";
        }

        List<String> blocks = new ArrayList<>();
        Set<String> emittedKeys = new LinkedHashSet<>();

        for (AssertionIntent intent : context.assertionIntents()) {
            AssertionIntentKind kind = intent.kind();

            switch (kind) {
                case PAGE_VISIBLE, PAGE_ACCESSIBLE, CONTENT_VISIBLE, PUBLIC_ACCESSIBLE, SUCCESS_STATE_VISIBLE ->
                        addBlock(blocks, emittedKeys, "success", stateAssertion(
                                context,
                                context.successStateMethodName(),
                                "Expected visible target page state"
                        ));
                case COLLECTION_VISIBLE, LISTING_VISIBLE ->
                        addBlock(blocks, emittedKeys, "listing", stateAssertion(
                                context,
                                "isListingContainerVisible",
                                "Expected listing container to be visible"
                        ));
                case NON_EMPTY_RESULTS, ENTITY_SUMMARY_VISIBLE, ITEM_CARDS_VISIBLE ->
                        addBlock(blocks, emittedKeys, "item-cards", stateAssertion(
                                context,
                                "hasVisibleItemCards",
                                "Expected visible item cards in the listing"
                        ));
                case ENTITY_CONTENT_VISIBLE, ITEM_CONTENT_VISIBLE ->
                        addBlock(blocks, emittedKeys, "item-content", stateAssertion(
                                context,
                                "isItemCardContentVisible",
                                "Expected visible basic product information in at least one item card"
                        ));
                case ENTITY_PRESENT_IN_CONTAINER ->
                        addBlock(blocks, emittedKeys, "container-item", containerItemAssertion(context));
                case ITEM_PRESENT_IN_CONTAINER ->
                        addBlock(blocks, emittedKeys, "container-item", containerItemAssertion(context));
                case CONTAINER_EMPTY ->
                        addBlock(blocks, emittedKeys, "container-empty", containerEmptyAssertion(context));
                case DETAILS_VISIBLE, DETAILS_OPENED -> {
                    addBlock(blocks, emittedKeys, "details", stateAssertion(
                            context,
                            context.detailsStateMethodName(),
                            "Expected details view to be visible"
                    ));
                    if (intent.expectedValue() != null) {
                        addBlock(blocks, emittedKeys, "url:" + intent.expectedValue(), urlContainsAssertion(
                                context,
                                intent.expectedValue()
                        ));
                    }
                }
                case ERROR_VISIBLE, AUTH_REQUIRED ->
                        addBlock(blocks, emittedKeys, "error", stateAssertion(
                                context,
                                context.errorStateMethodName(),
                                "Expected error, auth-required, or restricted state"
                        ));
                case URL_CONTAINS -> {
                    if (intent.expectedValue() != null) {
                        addBlock(blocks, emittedKeys, "url:" + intent.expectedValue(), urlContainsAssertion(
                                context,
                                intent.expectedValue()
                        ));
                    }
                }
            }
        }

        return blocks.isEmpty() ? "" : String.join(System.lineSeparator() + System.lineSeparator(), blocks);
    }

    private String buildScenarioDrivenAssertionBlock(UiScenarioTemplateContext context) {
        if (context.scenarioAssertions().isEmpty()) {
            return "";
        }

        List<String> blocks = new ArrayList<>();
        Set<String> emittedKeys = new LinkedHashSet<>();

        for (String assertion : context.scenarioAssertions()) {
            String normalized = assertion.toLowerCase();

            if (containsAny(normalized, "error", "validation", "restricted", "does not complete", "remains on")) {
                addBlock(blocks, emittedKeys, "error", negativeStateAssertion(
                        context,
                        "Expected error or validation state to be visible"
                ));
            }

            if (containsAny(normalized, "details", "details content")) {
                addBlock(blocks, emittedKeys, "details", detailsStateAssertion(
                        context,
                        "Expected details view to be visible"
                ));
            }

            if (containsAny(normalized,
                    "representative item",
                    "results are present",
                    "summary content",
                    "item card is visible",
                    "item cards are visible and interactable")) {
                addBlock(blocks, emittedKeys, "non-empty", nonEmptyStateAssertion(
                        context,
                        "Expected non-empty results or overview content"
                ));
            }

            if (containsAny(normalized,
                    "list of available items",
                    "listing container",
                    "collection area",
                    "collections page is reachable",
                    "reachable without authentication")) {
                addBlock(blocks, emittedKeys, "success", positiveStateAssertion(
                        context,
                        "Expected listing or target page content to be visible"
                ));
            }

            if (containsAny(normalized, "basic product information", "product information")) {
                addBlock(blocks, emittedKeys, "details", detailsStateAssertion(
                        context,
                        "Expected item content or item entry point to be visible"
                ));
            }

            if (containsAny(normalized,
                    "reachable",
                    "confirmation",
                    "authentication succeeds",
                    "authenticated area",
                    "submission succeeds",
                    "success",
                    "safe state",
                    "security-related behavior is visible",
                    "user leaves the authenticated area",
                    "authentication is required again")) {
                addBlock(blocks, emittedKeys, "success", positiveStateAssertion(
                        context,
                        "Expected successful target state after scenario execution"
                ));
            }

            if (containsAny(normalized, "present in the destination container", "present in the cart", "present in cart")) {
                addBlock(blocks, emittedKeys, "container-item", containerItemAssertion(context));
            }

            if (containsAny(normalized, "destination container is empty", "cart must be empty", "cart is empty")) {
                addBlock(blocks, emittedKeys, "container-empty", containerEmptyAssertion(context));
            }
        }

        return blocks.isEmpty() ? "" : String.join(System.lineSeparator() + System.lineSeparator(), blocks);
    }

    private String positiveStateAssertion(UiScenarioTemplateContext context, String message) {
        return stateAssertion(context, context.successStateMethodName(), message) + optionalUrlAssertion(context);
    }

    private String negativeStateAssertion(UiScenarioTemplateContext context, String message) {
        return stateAssertion(context, context.errorStateMethodName(), message) + optionalUrlAssertion(context);
    }

    private String negativeStateAbsenceAssertion(UiScenarioTemplateContext context, String message) {
        return """
                UiAssertions.assertFalse(
                        %s.%s(),
                        "%s"
                );
                """.formatted(
                context.assertionPageVariableName(),
                context.errorStateMethodName(),
                message
        );
    }

    private String detailsStateAssertion(UiScenarioTemplateContext context, String message) {
        return stateAssertion(context, context.detailsStateMethodName(), message) + optionalUrlAssertion(context);
    }

    private String nonEmptyStateAssertion(UiScenarioTemplateContext context, String message) {
        return """
                UiAssertions.assertFalse(
                        %s.%s(),
                        "%s"
                );
                """.formatted(
                context.assertionPageVariableName(),
                context.emptyStateMethodName(),
                message
        ) + optionalUrlAssertion(context);
    }

    private String containerItemAssertion(UiScenarioTemplateContext context) {
        return """
                ScenarioData containerData = scenarioData("%s");

                UiAssertions.assertTrue(
                        %s.isItemPresentInContainer(containerData.required("entityKey")),
                        "Expected selected item to be present in the destination container"
                );
                """.formatted(
                context.dataSetName(),
                context.assertionPageVariableName()
        );
    }

    private String containerEmptyAssertion(UiScenarioTemplateContext context) {
        return """
                UiAssertions.assertTrue(
                        %s.isDestinationContainerEmpty(),
                        "Expected destination container to be empty"
                );
                """.formatted(
                context.assertionPageVariableName()
        );
    }

    private String stateAssertion(UiScenarioTemplateContext context, String methodName, String message) {
        return """
                UiAssertions.assertTrue(
                        %s.%s(),
                        "%s"
                );
                """.formatted(
                context.assertionPageVariableName(),
                methodName,
                message
        );
    }

    private String optionalUrlAssertion(UiScenarioTemplateContext context) {
        if (!context.hasExpectedUrlFragment()) {
            return "";
        }

        return System.lineSeparator() + System.lineSeparator() + urlContainsAssertion(
                context,
                context.expectedUrlFragment()
        );
    }

    private String urlContainsAssertion(UiScenarioTemplateContext context, String fragment) {
        return """
                UiAssertions.assertUrlContains(
                        %s.getCurrentUrl(),
                        "%s"
                );
                """.formatted(
                context.assertionPageVariableName(),
                fragment
        );
    }

    private void addBlock(List<String> blocks, Set<String> emittedKeys, String key, String block) {
        if (emittedKeys.add(key)) {
            blocks.add(block);
        }
    }

    private boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (text.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
