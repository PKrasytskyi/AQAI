package ua.demo.agentlab.ui.selenium.writer;

import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class UiActionTemplateLibrary {

    public String buildActionBlock(UiOperationType operationType, UiScenarioTemplateContext context) {
        Objects.requireNonNull(operationType, "operationType must not be null");
        Objects.requireNonNull(context, "context must not be null");

        String intentDrivenBlock = buildIntentDrivenActionBlock(context);
        if (!intentDrivenBlock.isBlank()) {
            return intentDrivenBlock;
        }

        String scenarioDrivenBlock = buildScenarioDrivenActionBlock(context);
        if (!scenarioDrivenBlock.isBlank()) {
            return scenarioDrivenBlock;
        }

        return switch (operationType) {
            case PAGE_OPEN -> """
                    %s.%s();
                    """.formatted(
                    context.pageVariableName(),
                    context.openMethodName()
            );
            case AUTH_FLOW, AUTH_FLOW_NEGATIVE -> """
                    UserCredentials user = credentials("%s");

                    %s.%s();
                    %s.%s(user.username(), user.password());
                    """.formatted(
                    context.credentialsProfileName(),
                    context.pageVariableName(),
                    context.openMethodName(),
                    context.pageVariableName(),
                    context.primaryActionMethodName()
            );
            case FORM_SUBMIT, FORM_SUBMIT_NEGATIVE, ENTITY_CREATE -> """
                    ScenarioData data = scenarioData("%s");

                    %s.%s();
                    %s.fillFrom(data);
                    %s.%s();
                    """.formatted(
                    context.dataSetName(),
                    context.pageVariableName(),
                    context.openMethodName(),
                    context.pageVariableName(),
                    context.pageVariableName(),
                    context.primaryActionMethodName()
            );
            case FLOW_NAVIGATION, SESSION_END -> """
                    %s.%s();
                    %s.%s();
                    """.formatted(
                    context.pageVariableName(),
                    context.openMethodName(),
                    context.pageVariableName(),
                    context.primaryActionMethodName()
            );
            case DATA_SEARCH -> """
                    ScenarioData data = scenarioData("%s");

                    %s.%s();
                    %s.searchFor(data.required("query"));
                    """.formatted(
                    context.dataSetName(),
                    context.pageVariableName(),
                    context.openMethodName(),
                    context.pageVariableName()
            );
            case DATA_FILTER -> """
                    ScenarioData data = scenarioData("%s");

                    %s.%s();
                    %s.applyFilter(data.required("filterValue"));
                    """.formatted(
                    context.dataSetName(),
                    context.pageVariableName(),
                    context.openMethodName(),
                    context.pageVariableName()
            );
            case FILE_UPLOAD -> """
                    ScenarioData data = scenarioData("%s");

                    %s.%s();
                    %s.uploadFile(data.required("filePath"));
                    """.formatted(
                    context.dataSetName(),
                    context.pageVariableName(),
                    context.openMethodName(),
                    context.pageVariableName()
            );
            case FILE_DOWNLOAD -> """
                    %s.%s();
                    %s.downloadFile();
                    """.formatted(
                    context.pageVariableName(),
                    context.openMethodName(),
                    context.pageVariableName()
            );
            case DETAILS_OPEN -> """
                    ScenarioData data = scenarioData("%s");

                    %s.%s();
                    %s.openDetailsFor(data.required("entityKey"));
                    """.formatted(
                    context.dataSetName(),
                    context.pageVariableName(),
                    context.openMethodName(),
                    context.pageVariableName()
            );
            case ENTITY_UPDATE -> """
                    ScenarioData data = scenarioData("%s");

                    %s.%s();
                    %s.openDetailsFor(data.required("entityKey"));
                    %s.fillFrom(data);
                    %s.%s();
                    """.formatted(
                    context.dataSetName(),
                    context.pageVariableName(),
                    context.openMethodName(),
                    context.pageVariableName(),
                    context.pageVariableName(),
                    context.pageVariableName(),
                    context.primaryActionMethodName()
            );
            case ENTITY_DELETE -> """
                    ScenarioData data = scenarioData("%s");

                    %s.%s();
                    %s.deleteByKey(data.required("entityKey"));
                    """.formatted(
                    context.dataSetName(),
                    context.pageVariableName(),
                    context.openMethodName(),
                    context.pageVariableName()
            );
            case SECURITY_STEP -> """
                    %s.%s();
                    %s.%s();
                    """.formatted(
                    context.pageVariableName(),
                    context.openMethodName(),
                    context.pageVariableName(),
                    context.primaryActionMethodName()
            );
        };
    }

    private String buildIntentDrivenActionBlock(UiScenarioTemplateContext context) {
        if (context.operationIntents().isEmpty()) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        boolean opened = false;
        boolean credentialsDeclared = false;
        boolean scenarioDataDeclared = false;
        boolean loginInvoked = false;
        boolean fillInvoked = false;

        for (UiOperationIntent intent : context.operationIntents()) {
            UiOperationKind kind = intent.kind();

            switch (kind) {
                case OPEN_PAGE, VERIFY_PAGE_ACCESS, INSPECT_PAGE_CONTENT, INSPECT_COLLECTION,
                        INSPECT_ENTITY_SUMMARY, REVIEW_ENTITY_CONTENT,
                        VERIFY_PUBLIC_ACCESS, INSPECT_LISTING, INSPECT_ITEM_CARDS, REVIEW_ITEM_CONTENT -> {
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                }
                case OPEN_TARGET_CONTAINER, OPEN_DESTINATION_CONTAINER -> {
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    addLine(lines, emitted, "%s.openDestinationContainer();".formatted(context.pageVariableName()));
                }
                case ADD_ENTITY_TO_CONTAINER, ADD_ITEM_TO_CONTAINER -> {
                    if (!scenarioDataDeclared) {
                        addLine(lines, emitted, "ScenarioData data = scenarioData(\"%s\");".formatted(
                                context.dataSetName()
                        ));
                        scenarioDataDeclared = true;
                    }
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    addLine(lines, emitted, "%s.addItemToContainer(data.required(\"%s\"));".formatted(
                            context.pageVariableName(),
                            intent.dataKey() == null ? "entityKey" : intent.dataKey()
                    ));
                }
                case REMOVE_ENTITY_FROM_CONTAINER, REMOVE_ITEM_FROM_CONTAINER -> {
                    if (!scenarioDataDeclared) {
                        addLine(lines, emitted, "ScenarioData data = scenarioData(\"%s\");".formatted(
                                context.dataSetName()
                        ));
                        scenarioDataDeclared = true;
                    }
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    addLine(lines, emitted, "%s.removeItemFromContainer(data.required(\"%s\"));".formatted(
                            context.pageVariableName(),
                            intent.dataKey() == null ? "entityKey" : intent.dataKey()
                    ));
                }
                case OPEN_DETAILS -> {
                    if (!scenarioDataDeclared) {
                        addLine(lines, emitted, "ScenarioData data = scenarioData(\"%s\");".formatted(
                                context.dataSetName()
                        ));
                        scenarioDataDeclared = true;
                    }
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    addLine(lines, emitted, "%s.openDetailsFor(data.required(\"%s\"));".formatted(
                            context.pageVariableName(),
                            intent.dataKey() == null ? "entityKey" : intent.dataKey()
                    ));
                }
                case AUTHENTICATE -> {
                    if (!credentialsDeclared) {
                        addLine(lines, emitted, "UserCredentials user = credentials(\"%s\");".formatted(
                                context.credentialsProfileName()
                        ));
                        credentialsDeclared = true;
                    }
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    if (!loginInvoked) {
                        addLine(lines, emitted, "%s.login(user.username(), user.password());".formatted(
                                context.pageVariableName()
                        ));
                        loginInvoked = true;
                    }
                }
                case SUBMIT_FORM -> {
                    if (!scenarioDataDeclared) {
                        addLine(lines, emitted, "ScenarioData data = scenarioData(\"%s\");".formatted(
                                context.dataSetName()
                        ));
                        scenarioDataDeclared = true;
                    }
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    if (!fillInvoked) {
                        addLine(lines, emitted, "%s.fillFrom(data);".formatted(context.pageVariableName()));
                        fillInvoked = true;
                    }
                    addLine(lines, emitted, "%s.%s();".formatted(
                            context.pageVariableName(),
                            context.primaryActionMethodName()
                    ));
                }
                case SEARCH -> {
                    if (!scenarioDataDeclared) {
                        addLine(lines, emitted, "ScenarioData data = scenarioData(\"%s\");".formatted(
                                context.dataSetName()
                        ));
                        scenarioDataDeclared = true;
                    }
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    addLine(lines, emitted, "%s.searchFor(data.required(\"%s\"));".formatted(
                            context.pageVariableName(),
                            intent.dataKey() == null ? "query" : intent.dataKey()
                    ));
                }
                case FILTER -> {
                    if (!scenarioDataDeclared) {
                        addLine(lines, emitted, "ScenarioData data = scenarioData(\"%s\");".formatted(
                                context.dataSetName()
                        ));
                        scenarioDataDeclared = true;
                    }
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    addLine(lines, emitted, "%s.applyFilter(data.required(\"%s\"));".formatted(
                            context.pageVariableName(),
                            intent.dataKey() == null ? "filterValue" : intent.dataKey()
                    ));
                }
                case SORT -> {
                    if (!scenarioDataDeclared) {
                        addLine(lines, emitted, "ScenarioData data = scenarioData(\"%s\");".formatted(
                                context.dataSetName()
                        ));
                        scenarioDataDeclared = true;
                    }
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    addLine(lines, emitted, "%s.applySort(data.required(\"%s\"));".formatted(
                            context.pageVariableName(),
                            intent.dataKey() == null ? "sortValue" : intent.dataKey()
                    ));
                }
                case LOGOUT -> {
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    addLine(lines, emitted, "%s.logout();".formatted(context.pageVariableName()));
                }
                case UPLOAD_FILE -> {
                    if (!scenarioDataDeclared) {
                        addLine(lines, emitted, "ScenarioData data = scenarioData(\"%s\");".formatted(
                                context.dataSetName()
                        ));
                        scenarioDataDeclared = true;
                    }
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    addLine(lines, emitted, "%s.uploadFile(data.required(\"%s\"));".formatted(
                            context.pageVariableName(),
                            intent.dataKey() == null ? "filePath" : intent.dataKey()
                    ));
                }
                case DOWNLOAD_FILE -> {
                    if (!opened) {
                        addLine(lines, emitted, "%s.%s();".formatted(
                                context.pageVariableName(),
                                context.openMethodName()
                        ));
                        opened = true;
                    }
                    addLine(lines, emitted, "%s.downloadFile();".formatted(context.pageVariableName()));
                }
            }
        }

        return lines.isEmpty() ? "" : String.join(System.lineSeparator(), lines);
    }

    private String buildScenarioDrivenActionBlock(UiScenarioTemplateContext context) {
        if (context.scenarioActions().isEmpty()) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        boolean opened = false;
        boolean credentialsDeclared = false;
        boolean scenarioDataDeclared = false;
        boolean loginInvoked = false;
        boolean fillInvoked = false;

        for (String action : context.scenarioActions()) {
            String normalized = action.toLowerCase();

            if (!opened && shouldOpenPage(normalized)) {
                addLine(lines, emitted, "%s.%s();".formatted(
                        context.pageVariableName(),
                        context.openMethodName()
                ));
                opened = true;
            }

            if (containsAny(normalized,
                    "credential",
                    "authenticate",
                    "sign in",
                    "authentication form",
                    "valid profile",
                    "authenticated area")) {
                if (!credentialsDeclared) {
                    addLine(lines, emitted, "UserCredentials user = credentials(\"%s\");".formatted(
                            context.credentialsProfileName()
                    ));
                    credentialsDeclared = true;
                }
                if (!opened) {
                    addLine(lines, emitted, "%s.%s();".formatted(
                            context.pageVariableName(),
                            context.openMethodName()
                    ));
                    opened = true;
                }
                if (!loginInvoked) {
                    addLine(lines, emitted, "%s.%s(user.username(), user.password());".formatted(
                            context.pageVariableName(),
                            "login"
                    ));
                    loginInvoked = true;
                }
                continue;
            }

            if (containsAny(normalized,
                    "fill the required fields",
                    "provide the required scenario data",
                    "submit the form with valid data",
                    "submit the form with incomplete or invalid data",
                    "submit the form with invalid or missing data")) {
                if (!scenarioDataDeclared) {
                    addLine(lines, emitted, "ScenarioData data = scenarioData(\"%s\");".formatted(
                            context.dataSetName()
                    ));
                    scenarioDataDeclared = true;
                }
                if (!opened) {
                    addLine(lines, emitted, "%s.%s();".formatted(
                            context.pageVariableName(),
                            context.openMethodName()
                    ));
                    opened = true;
                }
                if (!fillInvoked) {
                    addLine(lines, emitted, "%s.fillFrom(data);".formatted(context.pageVariableName()));
                    fillInvoked = true;
                }
                if (normalized.contains("submit")) {
                    addLine(lines, emitted, "%s.%s();".formatted(
                            context.pageVariableName(),
                            context.primaryActionMethodName()
                    ));
                }
                continue;
            }

            if (containsAny(normalized, "submit the form", "submit the recovery request")) {
                if (!opened) {
                    addLine(lines, emitted, "%s.%s();".formatted(
                            context.pageVariableName(),
                            context.openMethodName()
                    ));
                    opened = true;
                }
                addLine(lines, emitted, "%s.%s();".formatted(
                        context.pageVariableName(),
                        context.primaryActionMethodName()
                ));
                continue;
            }

            if (containsAny(normalized, "recovery entry point", "navigate to the recovery page")) {
                if (!opened) {
                    addLine(lines, emitted, "%s.%s();".formatted(
                            context.pageVariableName(),
                            context.openMethodName()
                    ));
                    opened = true;
                }
                addLine(lines, emitted, "%s.%s();".formatted(
                        context.pageVariableName(),
                        context.primaryActionMethodName()
                ));
                continue;
            }

            if (containsAny(normalized, "representative record", "details entry", "select a representative record")) {
                if (!scenarioDataDeclared) {
                    addLine(lines, emitted, "ScenarioData data = scenarioData(\"%s\");".formatted(
                            context.dataSetName()
                    ));
                    scenarioDataDeclared = true;
                }
                if (!opened) {
                    addLine(lines, emitted, "%s.%s();".formatted(
                            context.pageVariableName(),
                            context.openMethodName()
                    ));
                    opened = true;
                }
                addLine(lines, emitted, "%s.openDetailsFor(data.required(\"entityKey\"));".formatted(
                        context.pageVariableName()
                ));
                continue;
            }

            if (containsAny(normalized, "sign-out", "sign out", "logout", "log out")) {
                if (!opened) {
                    addLine(lines, emitted, "%s.%s();".formatted(
                            context.pageVariableName(),
                            context.openMethodName()
                    ));
                    opened = true;
                }
                addLine(lines, emitted, "%s.logout();".formatted(context.pageVariableName()));
            }
        }

        return lines.isEmpty() ? "" : String.join(System.lineSeparator(), lines);
    }

    private boolean shouldOpenPage(String normalizedAction) {
        return containsAny(normalizedAction,
                "open ",
                "navigate to",
                "enter an authenticated area",
                "open the overview area",
                "open the summary or list area",
                "open the target",
                "open loginpage");
    }

    private void addLine(List<String> lines, Set<String> emitted, String line) {
        if (emitted.add(line)) {
            lines.add(line);
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
