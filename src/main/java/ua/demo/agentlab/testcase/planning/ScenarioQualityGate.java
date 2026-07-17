package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.ui.contract.UiOperationKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ScenarioQualityGate {

    ScenarioQualityReport validate(ScenarioCandidate candidate) {
        List<String> issues = new ArrayList<>();
        if (candidate == null || candidate.primaryRequirement() == null) {
            return new ScenarioQualityReport(false, List.of("Scenario candidate is missing primary requirement."));
        }
        if (candidate.targetPageName().isBlank() || candidate.targetRoute().isBlank()) {
            issues.add("Target capability/page is not confirmed; scenario must remain needs-review.");
        }
        for (ScenarioAssertionCandidate assertion : candidate.assertions()) {
            if (assertion.assertionType() == AssertionType.URL_CONTAINS
                    && !assertion.route().isBlank()
                    && !assertion.expectedValue().isBlank()
                    && assertion.expectedValue().startsWith("/")
                    && !assertion.expectedValue().equals(assertion.route())) {
                issues.add("Route assertion expectedValue does not match assertion owner route.");
            }
            String expected = normalize(assertion.expectedValue());
            if (containsAny(expected, "username", "password", "login button", "login page")
                    && containsAny(normalize(assertion.ownerPage()), "dashboard", "authenticated", "secure")) {
                issues.add("Login assertion cannot be owned by authenticated page.");
            }
        }
        boolean performsLogout = candidate.steps().stream().anyMatch(step -> step.kind() == UiOperationKind.LOGOUT);
        boolean expectsLogoutVisible = candidate.assertions().stream()
                .anyMatch(assertion -> normalize(assertion.expectedValue()).contains("logout")
                        && containsAny(normalize(assertion.expectedValue()), "visible", "see", "available"));
        if (performsLogout && expectsLogoutVisible) {
            issues.add("Scenario performs LOGOUT but asserts logout action remains visible.");
        }
        return new ScenarioQualityReport(issues.isEmpty(), issues);
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(normalize(fragment))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
