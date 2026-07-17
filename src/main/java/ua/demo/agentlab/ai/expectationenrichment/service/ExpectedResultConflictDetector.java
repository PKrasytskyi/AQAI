package ua.demo.agentlab.ai.expectationenrichment.service;

import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.contract.UiOperationKind;
import ua.demo.agentlab.ai.expectationenrichment.model.ExpectedResultCandidate;

import java.util.List;
import java.util.Locale;

class ExpectedResultConflictDetector {

    List<String> detect(CanonicalTestCase testCase) {
        if (testCase == null) {
            return List.of();
        }
        java.util.ArrayList<String> conflicts = new java.util.ArrayList<>();
        String assertionText = normalize(String.join(" ", testCase.assertions()) + " "
                + testCase.assertionIntents().stream()
                .map(intent -> intent.expectedValue() == null ? "" : intent.expectedValue())
                .collect(java.util.stream.Collectors.joining(" ")) + " "
                + testCase.title());
        boolean performsLogout = testCase.operationIntents().stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.LOGOUT)
                || containsAny(normalize(String.join(" ", testCase.actions())), "logout", "sign out", "signout");
        boolean expectsLogoutVisible = containsAny(assertionText, "logout action is visible", "logout visible", "logout action");
        if (performsLogout && expectsLogoutVisible) {
            conflicts.add("Test performs LOGOUT but expected result asserts logout action remains visible.");
        }
        boolean logoutTargetsLoginPage = testCase.operationIntents().stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.LOGOUT
                        && intent.target() != null
                        && normalize(intent.target()).contains("login"));
        if (logoutTargetsLoginPage) {
            conflicts.add("LOGOUT operation is owned by an authenticated page, not LoginPage.");
        }
        if (testCase.pageName() == null || testCase.pageName().isBlank()
                || testCase.route() == null || testCase.route().isBlank()) {
            conflicts.add("Target capability/page route is not confirmed.");
        }
        String businessText = normalize(testCase.title() + " " + String.join(" ", testCase.actions())
                + " " + String.join(" ", testCase.assertions()));
        if (containsAny(businessText, "recruitment", "vacancy", "vacancies", "hiring manager", "selected filters")
                && containsAny(normalize(testCase.route()), "/auth/login", "/login")) {
            conflicts.add("Protected business action is incorrectly assigned to the login route.");
        }
        return conflicts.stream().distinct().toList();
    }

    List<String> detect(CanonicalTestCase testCase, ExpectedResultCandidate candidate) {
        java.util.ArrayList<String> conflicts = new java.util.ArrayList<>(detect(testCase));
        if (testCase == null || candidate == null) {
            return conflicts.stream().distinct().toList();
        }
        String expected = normalize(candidate.expectedResult());
        boolean performsSearch = testCase.operationIntents().stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.SEARCH);
        boolean appliesFilter = testCase.operationIntents().stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.FILTER);
        if (performsSearch && !containsAny(expected, "result", "matching", "applies")) {
            conflicts.add("SEARCH operation requires a result-state expectation, not only control visibility.");
        }
        if (appliesFilter && !containsAny(expected, "filter", "result", "matching", "displayed")) {
            conflicts.add("FILTER operation requires a filter or result-state expectation.");
        }
        return conflicts.stream().distinct().toList();
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (!normalize(fragment).isBlank() && value.contains(normalize(fragment))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
