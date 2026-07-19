package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.ai.assertions.model.AssertionType;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

class ExpectedResultContractCatalog {

    private final List<ExpectedResultContract> contracts;

    ExpectedResultContractCatalog(List<ExpectedResultContract> contracts) {
        this.contracts = contracts == null ? List.of() : List.copyOf(contracts);
    }

    Optional<ExpectedResultContract> exact(String requirementId) {
        return contracts.stream()
                .filter(contract -> contract.requirementId().equals(requirementId))
                .findFirst();
    }

    List<ExpectedResultContract> exactAll(String requirementId) {
        return contracts.stream()
                .filter(contract -> contract.requirementId().equals(requirementId))
                .toList();
    }

    Optional<ExpectedResultContract> compatibleFor(RequirementUnit unit) {
        if (unit == null) {
            return Optional.empty();
        }
        Optional<ExpectedResultContract> exact = exact(unit.requirement().id());
        if (exact.isPresent()) {
            return exact;
        }
        return contracts.stream()
                .filter(contract -> compatible(unit, contract))
                .max(Comparator.comparingDouble(contract -> score(unit, contract)));
    }

    private boolean compatible(RequirementUnit unit, ExpectedResultContract contract) {
        if (!contract.ownerPage().isBlank() && !unit.ownerPage().isBlank()
                && !contract.ownerPage().equalsIgnoreCase(unit.ownerPage())) {
            return false;
        }
        if (!contract.route().isBlank() && !unit.route().isBlank()
                && !contract.route().equalsIgnoreCase(unit.route())) {
            return false;
        }
        if (!subjectCompatible(unit, contract)) {
            return false;
        }
        String expected = normalize(contract.expectedValue());
        return switch (unit.capability()) {
            case AUTHENTICATION, FORM -> containsAny(expected, "login", "username", "password", "credential", unit.route());
            case AUTHENTICATED_AREA -> containsAny(expected, "authenticated", "dashboard", "welcome", unit.route());
            case LOGOUT -> containsAny(expected, "logout", "sign out");
            case NAVIGATION -> containsAny(expected, "home", "accessible", "route", unit.route());
            default -> true;
        };
    }

    private boolean subjectCompatible(RequirementUnit unit, ExpectedResultContract contract) {
        String unitText = normalize(unit.requirement().title() + " " + unit.requirement().statement());
        String expected = normalize(contract.expectedValue());
        List<String> subjectTokens = List.of(
                "username",
                "password",
                "login button",
                "logout",
                "sign out",
                "dashboard",
                "authenticated",
                "welcome",
                "home page",
                "login page",
                "route"
        );
        List<String> unitSubjects = subjectTokens.stream()
                .filter(token -> unitText.contains(token))
                .toList();
        if (unitSubjects.isEmpty()) {
            return true;
        }
        boolean hasExactSubject = unitSubjects.stream().anyMatch(expected::contains);
        if (hasExactSubject) {
            return true;
        }
        if (unit.intent() == RequirementIntent.SUBMIT_FORM || unit.intent() == RequirementIntent.AUTHENTICATE) {
            return containsAny(expected, "authenticated", "dashboard", "redirected", "logged");
        }
        return false;
    }

    private double score(RequirementUnit unit, ExpectedResultContract contract) {
        double score = contract.confidence();
        if (contract.ownerPage().equalsIgnoreCase(unit.ownerPage())) {
            score += 0.10d;
        }
        if (contract.route().equalsIgnoreCase(unit.route())) {
            score += 0.10d;
        }
        String text = normalize(unit.requirement().title() + " " + unit.requirement().statement());
        for (String token : normalize(contract.expectedValue()).split("[^a-z0-9/_-]+")) {
            if (token.length() >= 4 && text.contains(token)) {
                score += 0.02d;
            }
        }
        return Math.min(1.0d, score);
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (fragment != null && !fragment.isBlank() && value.contains(normalize(fragment))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
