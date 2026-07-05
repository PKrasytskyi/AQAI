package ua.demo.agentlab.ai.expectationenrichment.service;

import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.contract.UiOperationKind;

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
