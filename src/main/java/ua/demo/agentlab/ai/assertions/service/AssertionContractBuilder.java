package ua.demo.agentlab.ai.assertions.service;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.assertions.model.AssertionSource;
import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;

import java.util.ArrayList;
import java.util.List;

public class AssertionContractBuilder {

    public List<AssertionContract> build(List<CanonicalTestCase> testCases) {
        if (testCases == null) {
            return List.of();
        }
        List<AssertionContract> contracts = new ArrayList<>();
        for (CanonicalTestCase testCase : testCases) {
            for (AssertionIntent intent : testCase.assertionIntents()) {
                contracts.add(toContract(testCase, intent));
            }
        }
        return contracts.stream().distinct().toList();
    }

    private AssertionContract toContract(CanonicalTestCase testCase, AssertionIntent intent) {
        AssertionType type = resolveAssertionType(testCase, intent);
        String expectedValue = expectedValue(testCase, intent, type);
        AssertionOwner owner = resolveOwner(testCase, type, expectedValue);
        return new AssertionContract(
                firstRequirementId(testCase),
                testCase.id(),
                type,
                expectedValue,
                owner.pageName(),
                owner.route(),
                testCase.sourceReference(),
                confidence(type, expectedValue),
                source(type, expectedValue)
        );
    }

    private AssertionOwner resolveOwner(CanonicalTestCase testCase, AssertionType type, String expectedValue) {
        String normalized = normalize(expectedValue + " " + firstAssertion(testCase) + " " + testCase.title());
        if (type == AssertionType.URL_CONTAINS && expectedValue != null && !expectedValue.isBlank()) {
            if (expectedValue.contains("/login") || normalized.contains("login page")) {
                return sourceOwner(testCase, "LoginPage", "");
            }
            if (expectedValue.contains("/secure")
                    || expectedValue.contains("/dashboard")
                    || normalized.contains("authenticated area")) {
                return targetOwner(testCase, "AuthenticatedAreaPage", "");
            }
        }
        if (containsAny(normalized, "login page", "login form", "username", "password", "login button")) {
            return sourceOwner(testCase, "LoginPage", "");
        }
        if (containsAny(normalized, "authenticated area", "secure area", "welcome message", "logged with valid credentials", "logout action")) {
            return targetOwner(testCase, "AuthenticatedAreaPage", "");
        }
        return targetOwner(testCase, testCase.pageName(), testCase.route());
    }

    private AssertionType resolveAssertionType(CanonicalTestCase testCase, AssertionIntent intent) {
        String text = normalize(firstNonBlank(
                intent == null ? "" : intent.expectedValue(),
                firstAssertion(testCase),
                testCase.title()
        ));
        if (containsAny(text, "route contains", "route matches", "current url contains", "redirected to")) {
            return AssertionType.URL_CONTAINS;
        }
        if (containsAny(text, "page is accessible", "home page is accessible")) {
            return AssertionType.URL_CONTAINS;
        }
        if (containsAny(text, "field is visible", "button is visible", "action is visible", "form is visible")) {
            return AssertionType.ELEMENT_VISIBLE;
        }
        if (containsAny(text, "logged with valid credentials", "successful login state")) {
            return AssertionType.AUTHENTICATED_AREA_VISIBLE;
        }
        return toAssertionType(intent == null ? null : intent.kind());
    }

    private AssertionOwner sourceOwner(CanonicalTestCase testCase, String fallbackPage, String fallbackRoute) {
        return new AssertionOwner(
                firstNonBlank(testCase.sourcePageName(), fallbackPage),
                firstNonBlank(testCase.sourceRoute(), fallbackRoute)
        );
    }

    private AssertionOwner targetOwner(CanonicalTestCase testCase, String fallbackPage, String fallbackRoute) {
        return new AssertionOwner(
                firstNonBlank(testCase.pageName(), fallbackPage),
                firstNonBlank(testCase.route(), fallbackRoute)
        );
    }

    private AssertionType toAssertionType(AssertionIntentKind kind) {
        if (kind == null) {
            return AssertionType.ELEMENT_VISIBLE;
        }
        return switch (kind) {
            case URL_CONTAINS -> AssertionType.URL_CONTAINS;
            case ERROR_VISIBLE, AUTH_REQUIRED -> AssertionType.ERROR_MESSAGE_VISIBLE;
            case SUCCESS_STATE_VISIBLE -> AssertionType.AUTHENTICATED_AREA_VISIBLE;
            case PAGE_VISIBLE, PAGE_ACCESSIBLE, PUBLIC_ACCESSIBLE,
                    COLLECTION_VISIBLE, LISTING_VISIBLE, ITEM_CARDS_VISIBLE,
                    ENTITY_SUMMARY_VISIBLE, ENTITY_CONTENT_VISIBLE, DETAILS_VISIBLE,
                    DETAILS_OPENED, ITEM_CONTENT_VISIBLE, NON_EMPTY_RESULTS -> AssertionType.ELEMENT_VISIBLE;
            case CONTENT_VISIBLE -> AssertionType.TEXT_VISIBLE;
            case ENTITY_PRESENT_IN_CONTAINER, ITEM_PRESENT_IN_CONTAINER,
                    CONTAINER_EMPTY -> AssertionType.DATA_STATE_MATCHES;
        };
    }

    private String expectedValue(CanonicalTestCase testCase, AssertionIntent intent, AssertionType type) {
        if (type == AssertionType.URL_CONTAINS || type == AssertionType.ROUTE_EQUALS) {
            return firstNonBlank(routeLike(intent.expectedValue()), testCase.route(), testCase.sourceRoute());
        }
        String text = normalize(firstNonBlank(testCase.title(), firstAssertion(testCase), intent.expectedValue()));
        if (type == AssertionType.ELEMENT_VISIBLE || type == AssertionType.FORM_VISIBLE) {
            if (containsAny(text, "username field")) {
                return "usernameInput";
            }
            if (containsAny(text, "password field")) {
                return "passwordInput";
            }
            if (containsAny(text, "login button")) {
                return "loginButton";
            }
            if (containsAny(text, "logout action")) {
                return "logoutLink";
            }
            if (containsAny(text, "welcome message")) {
                return "welcomeMessage";
            }
        }
        return firstNonBlank(intent.expectedValue(), firstAssertion(testCase), testCase.title());
    }

    private double confidence(AssertionType type, String expectedValue) {
        if (type == AssertionType.URL_CONTAINS || type == AssertionType.ROUTE_EQUALS) {
            return expectedValue.isBlank() ? 0.75d : 1.0d;
        }
        return expectedValue.isBlank() ? 0.65d : 0.88d;
    }

    private AssertionSource source(AssertionType type, String expectedValue) {
        if (type == AssertionType.URL_CONTAINS || type == AssertionType.ROUTE_EQUALS) {
            return AssertionSource.PROJECT_PROFILE;
        }
        return expectedValue.isBlank() ? AssertionSource.RULE_BASED : AssertionSource.REQUIREMENT;
    }

    private String firstRequirementId(CanonicalTestCase testCase) {
        return testCase.requirementRefs().isEmpty() ? "" : testCase.requirementRefs().get(0);
    }

    private String firstAssertion(CanonicalTestCase testCase) {
        return testCase.assertions().isEmpty() ? "" : testCase.assertions().get(0);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String routeLike(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(/[A-Za-z0-9._~!$&'()*+,;=:@%/-]+)")
                .matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (fragment != null && !fragment.isBlank() && text.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private record AssertionOwner(String pageName, String route) {
    }
}
