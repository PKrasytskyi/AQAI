package ua.demo.agentlab.ui.testcontract.assembly;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepSpec;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;
import ua.demo.agentlab.ui.testcontract.model.UiTestActionSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestArgumentSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestAssertionMode;
import ua.demo.agentlab.ui.testcontract.model.UiTestAssertionSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractBundle;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestDataReferenceSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestDataReferenceType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UiTestContractAssembler {

    private static final String VALID_CREDENTIALS_ID = "validCredentials";
    private static final String VALID_CREDENTIALS_KEY = "valid-user";

    public UiTestContractBundle assemble(UiTestContractAssemblyInput input) {
        if (input == null || input.canonicalTestCases() == null) {
            return new UiTestContractBundle(null, List.of());
        }
        List<UiTestContractSpec> contracts = input.canonicalTestCases().testCases().stream()
                .sorted(Comparator.comparing(CanonicalTestCase::id))
                .map(testCase -> assemble(testCase, input.assertionContracts(), input.pomContracts()))
                .toList();
        return new UiTestContractBundle(null, contracts);
    }

    private UiTestContractSpec assemble(
            CanonicalTestCase testCase,
            List<AssertionContract> assertionContracts,
            List<PomContractSpec> pomContracts
    ) {
        List<String> gaps = new ArrayList<>();
        List<UiTestActionSpec> preconditions = new ArrayList<>();
        List<UiTestActionSpec> actions = new ArrayList<>();
        Map<String, UiTestDataReferenceSpec> dataReferences = new LinkedHashMap<>();
        boolean authenticationRequired = testCase.operationIntents().stream()
                .anyMatch(operation -> operation.kind() == UiOperationKind.AUTHENTICATE);

        if (authenticationRequired) {
            resolveAuthenticationPage(pomContracts).ifPresentOrElse(
                    page -> preconditions.add(openInvocation(page, "AUTHENTICATION_SOURCE")),
                    () -> gaps.add("AUTHENTICATE has no confirmed authentication page open method")
            );
        }

        List<UiOperationIntent> operations = testCase.operationIntents();
        for (int index = 0; index < operations.size(); index++) {
            UiOperationIntent operation = operations.get(index);
            List<ResolvedAction> resolved = resolveOperation(operation, testCase, pomContracts);
            if (resolved.isEmpty()) {
                gaps.add(operation.kind() + " has no confirmed page-object action method");
                continue;
            }
            boolean setup = index < operations.size() - 1;
            List<UiTestActionSpec> target = setup ? preconditions : actions;
            for (ResolvedAction action : resolved) {
                if (containsInvocation(target, action.pageName(), action.methodName())) {
                    continue;
                }
                target.add(new UiTestActionSpec(
                        0,
                        action.pageName(),
                        action.methodName(),
                        argumentsFor(action.action(), operation, testCase, dataReferences),
                        List.of(operation.kind().name())
                ));
            }
        }

        List<UiTestAssertionSpec> assertions = resolveAssertions(
                testCase,
                assertionContracts,
                pomContracts,
                gaps
        );
        return new UiTestContractSpec(
                testCase.id(),
                safe(testCase.canonicalFlowType()),
                typeName(testCase.id() + " " + testCase.title()) + "Test",
                "should" + typeName(testCase.title()),
                testCase.title(),
                safe(testCase.sourcePageName()),
                safe(testCase.pageName()),
                ordered(preconditions),
                ordered(actions),
                assertions,
                List.copyOf(dataReferences.values()),
                testCase.requirementRefs().isEmpty() ? List.of(testCase.id()) : testCase.requirementRefs(),
                gaps.stream().distinct().toList()
        );
    }

    private List<ResolvedAction> resolveOperation(
            UiOperationIntent operation,
            CanonicalTestCase testCase,
            List<PomContractSpec> pomContracts
    ) {
        if (operation.kind() == UiOperationKind.OPEN_PAGE) {
            return resolvePage(testCase.pageName(), testCase.route(), pomContracts)
                    .map(page -> List.of(new ResolvedAction(page.page().name(), page.page().openMethod(), null)))
                    .orElse(List.of());
        }
        if (operation.kind() == UiOperationKind.AUTHENTICATE) {
            return resolveAuthenticationActions(pomContracts);
        }
        List<PomContractSpec> preferredPages = preferredPages(testCase, pomContracts);
        ResolvedAction best = null;
        int bestScore = 0;
        for (PomContractSpec page : preferredPages) {
            for (PomActionSpec action : page.actions()) {
                int score = actionScore(operation, action, page);
                if (score > bestScore) {
                    bestScore = score;
                    best = new ResolvedAction(page.page().name(), action.methodName(), action);
                }
            }
        }
        return best == null || bestScore < 3 ? List.of() : List.of(best);
    }

    private java.util.Optional<PomContractSpec> resolveAuthenticationPage(List<PomContractSpec> pomContracts) {
        return pomContracts.stream()
                .filter(this::isAuthenticationPage)
                .sorted(Comparator.comparing(contract -> contract.page().name()))
                .findFirst();
    }

    private boolean isAuthenticationPage(PomContractSpec contract) {
        boolean password = contract.locators().stream().anyMatch(locator -> containsAny(
                locator.role() + " " + locator.id() + " " + locator.elementName(),
                Set.of("password")
        ));
        boolean typedInput = contract.actions().stream().anyMatch(action -> !action.parameters().isEmpty());
        boolean submit = contract.actions().stream().anyMatch(action -> containsAny(
                actionText(action, contract),
                Set.of("login", "submit", "authenticate", "signin")
        ));
        return password && typedInput && submit && !contract.page().openMethod().isBlank();
    }

    private List<ResolvedAction> resolveAuthenticationActions(List<PomContractSpec> pomContracts) {
        PomContractSpec page = resolveAuthenticationPage(pomContracts).orElse(null);
        if (page == null) {
            return List.of();
        }
        List<ResolvedAction> inputActions = page.actions().stream()
                .filter(action -> !action.parameters().isEmpty())
                .sorted(Comparator.comparingInt(action -> authenticationInputOrder(action, page)))
                .map(action -> new ResolvedAction(page.page().name(), action.methodName(), action))
                .toList();
        PomActionSpec submit = page.actions().stream()
                .filter(action -> action.parameters().isEmpty())
                .max(Comparator.comparingInt(action -> authenticationSubmitScore(action, page)))
                .filter(action -> authenticationSubmitScore(action, page) >= 3)
                .orElse(null);
        List<ResolvedAction> resolved = new ArrayList<>(inputActions);
        if (submit != null) {
            resolved.add(new ResolvedAction(page.page().name(), submit.methodName(), submit));
        }
        return resolved;
    }

    private int authenticationInputOrder(PomActionSpec action, PomContractSpec page) {
        String text = actionText(action, page);
        if (text.contains("username") || text.contains("email") || text.contains("user")) {
            return 0;
        }
        if (text.contains("password") || text.contains("pass")) {
            return 1;
        }
        return 2;
    }

    private int authenticationSubmitScore(PomActionSpec action, PomContractSpec page) {
        String text = actionText(action, page);
        int score = 0;
        if (containsAny(text, Set.of("login", "signin", "authenticate"))) {
            score += 4;
        }
        if (text.contains("submit")) {
            score += 3;
        }
        if (action.steps().stream().anyMatch(step -> "CLICK".equals(step.action().name()))) {
            score += 1;
        }
        return score;
    }

    private int actionScore(UiOperationIntent operation, PomActionSpec action, PomContractSpec page) {
        Set<String> expected = operationTokens(operation.kind());
        String text = actionText(action, page) + " " + safe(operation.target()) + " " + safe(operation.dataKey());
        int score = 0;
        for (String token : expected) {
            if (text.contains(token)) {
                score += 3;
            }
        }
        if (operation.kind() == UiOperationKind.OPEN_MENU && text.contains("open") && text.contains("menu")) {
            score += 4;
        }
        if (operation.kind() == UiOperationKind.LOGOUT && text.contains("logout")) {
            score += 6;
        }
        return score;
    }

    private Set<String> operationTokens(UiOperationKind kind) {
        return switch (kind) {
            case OPEN_MENU -> Set.of("menu", "dropdown");
            case LOGOUT -> Set.of("logout", "signout");
            case ENTER_TEXT -> Set.of("enter", "type", "input");
            case SUBMIT_FORM -> Set.of("submit");
            case SEARCH -> Set.of("search");
            case FILTER -> Set.of("filter");
            case OPEN_RECORD, OPEN_DETAILS -> Set.of("open", "record", "detail");
            case CREATE_RECORD -> Set.of("create", "add");
            case EDIT_RECORD -> Set.of("edit", "update");
            case DELETE_RECORD -> Set.of("delete", "remove");
            case OPEN_MODAL -> Set.of("modal", "dialog");
            case CONFIRM_ACTION -> Set.of("confirm");
            case SORT, SORT_COLLECTION -> Set.of("sort");
            case PAGINATE -> Set.of("page", "next", "previous");
            case UPLOAD_FILE -> Set.of("upload");
            case DOWNLOAD_FILE -> Set.of("download");
            default -> Set.of(kind.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        };
    }

    private List<UiTestAssertionSpec> resolveAssertions(
            CanonicalTestCase testCase,
            List<AssertionContract> contracts,
            List<PomContractSpec> pomContracts,
            List<String> gaps
    ) {
        List<UiTestAssertionSpec> resolved = new ArrayList<>();
        Set<String> usedMethods = new LinkedHashSet<>();
        List<AssertionContract> scoped = contracts.stream()
                .filter(contract -> testCase.id().equals(contract.testCaseId()))
                .sorted(Comparator.comparing(AssertionContract::requirementId)
                        .thenComparing(contract -> contract.type().name())
                        .thenComparing(AssertionContract::expectedValue))
                .toList();
        for (AssertionContract contract : scoped) {
            ResolvedAssertion assertion = resolveAssertion(contract, pomContracts);
            if (assertion == null) {
                gaps.add(contract.type() + "(" + contract.expectedValue() + ") has no validated POM assertion method");
                continue;
            }
            String identity = assertion.pageName() + "#" + assertion.methodName();
            if (!usedMethods.add(identity)) {
                continue;
            }
            resolved.add(new UiTestAssertionSpec(
                    resolved.size() + 1,
                    assertion.pageName(),
                    assertion.methodName(),
                    UiTestAssertionMode.TRUE,
                    contract.expectedValue(),
                    contract.requirementId(),
                    contract.expectedValue()
            ));
        }
        return List.copyOf(resolved);
    }

    private ResolvedAssertion resolveAssertion(AssertionContract contract, List<PomContractSpec> pomContracts) {
        PomContractSpec owner = resolvePage(contract.ownerPage(), contract.route(), pomContracts).orElse(null);
        if (owner == null) {
            return null;
        }
        PomAssertionSpec best = null;
        int bestScore = 0;
        for (PomAssertionSpec assertion : owner.assertions()) {
            int score = assertionScore(contract, assertion);
            if (score > bestScore) {
                best = assertion;
                bestScore = score;
            }
        }
        return best == null || bestScore < 3
                ? null
                : new ResolvedAssertion(owner.page().name(), best.methodName());
    }

    private int assertionScore(AssertionContract contract, PomAssertionSpec assertion) {
        String text = normalize(assertion.methodName() + " " + assertion.checks().stream()
                .map(check -> check.locator() + " " + check.expectedValue() + " " + check.route())
                .reduce("", (left, right) -> left + " " + right));
        int score = tokenOverlap(normalize(contract.expectedValue()), text);
        if (isRouteAssertion(contract.type())) {
            boolean urlCheck = assertion.checks().stream().anyMatch(this::isUrlCheck);
            if (urlCheck) {
                score += 4;
            }
            if (!contract.route().isBlank() && text.contains(normalize(contract.route()))) {
                score += 4;
            }
        }
        if (contract.type() == AssertionType.ELEMENT_VISIBLE
                && assertion.checks().stream().anyMatch(check -> check.check() == PomCheckType.VISIBLE)) {
            score += 2;
        }
        if (Set.of(
                AssertionType.AUTHENTICATION_SUCCEEDED,
                AssertionType.AUTHENTICATED_AREA_ABSENT,
                AssertionType.AUTHENTICATED_AREA_VISIBLE
        ).contains(contract.type()) && assertion.checks().stream().anyMatch(this::isUrlCheck)) {
            score += 3;
        }
        if (contract.type() == AssertionType.AUTHENTICATED_AREA_VISIBLE
                && assertion.checks().stream().anyMatch(check -> check.check() == PomCheckType.VISIBLE)) {
            score += 2;
        }
        return score;
    }

    private boolean isRouteAssertion(AssertionType type) {
        return type == AssertionType.URL_CONTAINS
                || type == AssertionType.ROUTE_EQUALS
                || type == AssertionType.ROUTE_REACHED
                || type == AssertionType.ROUTE_CHANGED;
    }

    private boolean isUrlCheck(PomCheckSpec check) {
        return check.check() == PomCheckType.URL_CONTAINS || check.check() == PomCheckType.URL_EQUALS;
    }

    private int tokenOverlap(String expected, String actual) {
        Set<String> ignored = Set.of(
                "the", "is", "and", "a", "an", "to", "after", "before", "with", "for", "of",
                "visible", "enabled", "area", "page", "user", "input", "action"
        );
        int score = 0;
        for (String token : expected.split("[^a-z0-9]+")) {
            if (token.length() >= 4 && !ignored.contains(token) && actual.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private UiTestActionSpec openInvocation(PomContractSpec page, String sourceOperation) {
        return new UiTestActionSpec(
                0,
                page.page().name(),
                page.page().openMethod(),
                List.of(),
                List.of(sourceOperation)
        );
    }

    private List<UiTestArgumentSpec> argumentsFor(
            PomActionSpec action,
            UiOperationIntent operation,
            CanonicalTestCase testCase,
            Map<String, UiTestDataReferenceSpec> dataReferences
    ) {
        if (action == null || action.parameters().isEmpty()) {
            return List.of();
        }
        boolean credentials = operation.kind() == UiOperationKind.AUTHENTICATE;
        String referenceId = credentials ? VALID_CREDENTIALS_ID : "scenarioData";
        String dataKey = credentials
                ? VALID_CREDENTIALS_KEY
                : scenarioDataKey(operation, testCase);
        dataReferences.putIfAbsent(
                referenceId,
                new UiTestDataReferenceSpec(
                        referenceId,
                        credentials ? UiTestDataReferenceType.CREDENTIALS : UiTestDataReferenceType.SCENARIO_DATA,
                        dataKey
                )
        );
        return action.parameters().stream()
                .map(parameter -> UiTestArgumentSpec.dataReference(
                        parameter.name(),
                        referenceId,
                        credentials ? credentialField(parameter.name()) : parameter.name()
                ))
                .toList();
    }

    private String scenarioDataKey(UiOperationIntent operation, CanonicalTestCase testCase) {
        if (operation.dataKey() != null && !operation.dataKey().isBlank()) {
            return operation.dataKey().trim();
        }
        return safe(testCase.id()).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    private String credentialField(String parameterName) {
        String normalized = normalize(parameterName);
        if (normalized.contains("pass")) {
            return "password";
        }
        if (normalized.contains("user") || normalized.contains("email")) {
            return "username";
        }
        return parameterName;
    }

    private List<UiTestActionSpec> ordered(List<UiTestActionSpec> actions) {
        List<UiTestActionSpec> ordered = new ArrayList<>();
        int index = 1;
        for (UiTestActionSpec action : actions) {
            ordered.add(new UiTestActionSpec(
                    index++,
                    action.page(),
                    action.method(),
                    action.arguments(),
                    action.sourceOperations()
            ));
        }
        return List.copyOf(ordered);
    }

    private boolean containsInvocation(List<UiTestActionSpec> actions, String page, String method) {
        return actions.stream().anyMatch(action -> action.page().equals(page) && action.method().equals(method));
    }

    private List<PomContractSpec> preferredPages(CanonicalTestCase testCase, List<PomContractSpec> pomContracts) {
        List<PomContractSpec> preferred = new ArrayList<>();
        resolvePage(testCase.sourcePageName(), testCase.sourceRoute(), pomContracts).ifPresent(preferred::add);
        resolvePage(testCase.pageName(), testCase.route(), pomContracts).ifPresent(page -> {
            if (!preferred.contains(page)) {
                preferred.add(page);
            }
        });
        for (PomContractSpec page : pomContracts) {
            if (!preferred.contains(page)) {
                preferred.add(page);
            }
        }
        return preferred;
    }

    private java.util.Optional<PomContractSpec> resolvePage(
            String pageName,
            String route,
            List<PomContractSpec> contracts
    ) {
        return contracts.stream()
                .filter(contract -> (!safe(pageName).isBlank() && contract.page().name().equalsIgnoreCase(pageName))
                        || (!safe(route).isBlank() && contract.page().route().equals(route)))
                .sorted(Comparator.comparing(contract -> contract.page().name()))
                .findFirst();
    }

    private String actionText(PomActionSpec action, PomContractSpec page) {
        Map<String, PomLocatorSpec> locators = new LinkedHashMap<>();
        for (PomLocatorSpec locator : page.locators()) {
            locators.put(locator.id(), locator);
        }
        StringBuilder text = new StringBuilder(action.methodName());
        for (PomStepSpec step : action.steps()) {
            text.append(' ').append(step.locator());
            PomLocatorSpec locator = locators.get(step.locator());
            if (locator != null) {
                text.append(' ').append(locator.elementName()).append(' ').append(locator.role());
            }
        }
        action.parameters().forEach(parameter -> text.append(' ').append(parameter.name()));
        return normalize(text.toString());
    }

    private boolean containsAny(String value, Set<String> tokens) {
        String normalized = normalize(value);
        return tokens.stream().anyMatch(normalized::contains);
    }

    private String typeName(String value) {
        StringBuilder result = new StringBuilder();
        for (String token : safe(value).split("[^A-Za-z0-9]+")) {
            if (token.isBlank()) {
                continue;
            }
            result.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                result.append(token.substring(1));
            }
        }
        return result.length() == 0 ? "GeneratedScenario" : result.toString();
    }

    private String normalize(String value) {
        return safe(value)
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record ResolvedAction(String pageName, String methodName, PomActionSpec action) {
    }

    private record ResolvedAssertion(String pageName, String methodName) {
    }
}
