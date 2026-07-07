package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;
import ua.demo.agentlab.ui.contract.UiOperationKind;

import java.util.ArrayList;
import java.util.List;

class ScenarioCandidateBuilder {

    private final ScenarioPageResolver pageResolver;
    private final ExpectedResultContractCatalog expectedResults;

    ScenarioCandidateBuilder(ScenarioPageResolver pageResolver, ExpectedResultContractCatalog expectedResults) {
        this.pageResolver = pageResolver;
        this.expectedResults = expectedResults;
    }

    ScenarioCandidate build(RequirementUnit unit) {
        ExpectedResultContract expected = expectedResults.exact(unit.requirement().id())
                .or(() -> allowSupportingExpectedResult(unit) ? expectedResults.compatibleFor(unit) : java.util.Optional.empty())
                .orElse(null);
        String targetPage = targetPage(unit);
        String targetRoute = targetRoute(unit);
        String sourcePage = sourcePage(unit);
        String sourceRoute = sourceRoute(unit);
        List<ScenarioStepCandidate> steps = steps(unit, sourcePage, sourceRoute, targetPage, targetRoute);
        List<ScenarioAssertionCandidate> assertions = assertions(unit, expected, targetPage, targetRoute);
        List<String> supporting = expected == null || expected.requirementId().equals(unit.requirement().id())
                ? List.of()
                : List.of(expected.requirementId());
        List<String> risks = new ArrayList<>();
        if (expected == null && unit.type() == RequirementUnitType.FUNCTIONAL) {
            risks.add("No compatible expected-result contract was found; using primary requirement text.");
        }
        return new ScenarioCandidate(unit, supporting, sourcePage, sourceRoute, targetPage, targetRoute, steps, assertions, risks);
    }

    private boolean allowSupportingExpectedResult(RequirementUnit unit) {
        if (unit.type() != RequirementUnitType.FUNCTIONAL) {
            return true;
        }
        return switch (unit.intent()) {
            case OPEN_PAGE, NAVIGATE, VERIFY_PAGE_ACCESSIBLE, VERIFY_ROUTE, AUTHENTICATE, SUBMIT_FORM,
                 VERIFY_AUTHENTICATED_AREA, VERIFY_LOGOUT_AVAILABLE -> true;
            case ENTER_DATA, VERIFY_ELEMENT_VISIBLE, INSPECT_CONTENT, LOGOUT -> false;
        };
    }

    private List<ScenarioStepCandidate> steps(
            RequirementUnit unit,
            String sourcePage,
            String sourceRoute,
            String targetPage,
            String targetRoute
    ) {
        List<ScenarioStepCandidate> steps = new ArrayList<>();
        if (requiresAuthenticationSetup(unit)) {
            steps.add(new ScenarioStepCandidate(UiOperationKind.AUTHENTICATE,
                    pageResolver.pageFor(RequirementCapability.AUTHENTICATION),
                    pageResolver.routeFor(RequirementCapability.AUTHENTICATION),
                    null,
                    true));
        }

        switch (unit.intent()) {
            case OPEN_PAGE, NAVIGATE, VERIFY_PAGE_ACCESSIBLE, INSPECT_CONTENT, VERIFY_ELEMENT_VISIBLE, VERIFY_ROUTE -> {
                if (!requiresAuthenticationSetup(unit)) {
                    steps.add(new ScenarioStepCandidate(UiOperationKind.OPEN_PAGE, targetPage, targetRoute, null, false));
                }
            }
            case ENTER_DATA -> {
                steps.add(new ScenarioStepCandidate(UiOperationKind.OPEN_PAGE, targetPage, targetRoute, null, true));
                steps.add(new ScenarioStepCandidate(UiOperationKind.ENTER_TEXT, targetPage, targetRoute, entryDataKey(unit), false));
            }
            case SUBMIT_FORM -> {
                steps.add(new ScenarioStepCandidate(UiOperationKind.OPEN_PAGE, sourcePage, sourceRoute, null, true));
                steps.add(new ScenarioStepCandidate(UiOperationKind.SUBMIT_FORM, sourcePage, sourceRoute, "default", false));
            }
            case AUTHENTICATE -> steps.add(new ScenarioStepCandidate(UiOperationKind.AUTHENTICATE,
                    sourcePage,
                    sourceRoute,
                    null,
                    false));
            case VERIFY_AUTHENTICATED_AREA, VERIFY_LOGOUT_AVAILABLE -> {
                if (steps.isEmpty()) {
                    steps.add(new ScenarioStepCandidate(UiOperationKind.AUTHENTICATE,
                            pageResolver.pageFor(RequirementCapability.AUTHENTICATION),
                            pageResolver.routeFor(RequirementCapability.AUTHENTICATION),
                            null,
                            true));
                }
            }
            case LOGOUT -> {
                if (steps.isEmpty()) {
                    steps.add(new ScenarioStepCandidate(UiOperationKind.AUTHENTICATE,
                            pageResolver.pageFor(RequirementCapability.AUTHENTICATION),
                            pageResolver.routeFor(RequirementCapability.AUTHENTICATION),
                            null,
                            true));
                }
                steps.add(new ScenarioStepCandidate(UiOperationKind.LOGOUT, targetPage, targetRoute, null, false));
            }
        }
        return deduplicateSteps(steps);
    }

    private List<ScenarioAssertionCandidate> assertions(
            RequirementUnit unit,
            ExpectedResultContract expected,
            String targetPage,
            String targetRoute
    ) {
        AssertionType assertionType = expected == null ? defaultAssertionType(unit) : expected.assertionType();
        String expectedValue = expected == null
                ? defaultExpectedValue(unit, assertionType, targetRoute)
                : expected.expectedValue();
        String ownerPage = expected == null || expected.ownerPage().isBlank() ? targetPage : expected.ownerPage();
        String route = expected == null || expected.route().isBlank() ? targetRoute : expected.route();
        return List.of(new ScenarioAssertionCandidate(
                assertionIntent(assertionType, unit),
                assertionType,
                expectedValue,
                ownerPage,
                route,
                expected == null ? unit.requirement().id() : expected.requirementId()
        ));
    }

    private boolean requiresAuthenticationSetup(RequirementUnit unit) {
        return (unit.capability() == RequirementCapability.AUTHENTICATED_AREA
                || unit.capability() == RequirementCapability.LOGOUT)
                && unit.intent() != RequirementIntent.AUTHENTICATE
                && unit.intent() != RequirementIntent.SUBMIT_FORM;
    }

    private String targetPage(RequirementUnit unit) {
        if (unit.intent() == RequirementIntent.SUBMIT_FORM || unit.intent() == RequirementIntent.AUTHENTICATE) {
            return pageResolver.pageFor(RequirementCapability.AUTHENTICATED_AREA);
        }
        return unit.ownerPage();
    }

    private String targetRoute(RequirementUnit unit) {
        if (unit.intent() == RequirementIntent.SUBMIT_FORM || unit.intent() == RequirementIntent.AUTHENTICATE) {
            return pageResolver.routeFor(RequirementCapability.AUTHENTICATED_AREA);
        }
        return unit.route();
    }

    private String sourcePage(RequirementUnit unit) {
        return unit.intent() == RequirementIntent.SUBMIT_FORM
                || unit.intent() == RequirementIntent.AUTHENTICATE
                || requiresAuthenticationSetup(unit)
                ? pageResolver.pageFor(RequirementCapability.AUTHENTICATION)
                : unit.ownerPage();
    }

    private String sourceRoute(RequirementUnit unit) {
        return unit.intent() == RequirementIntent.SUBMIT_FORM
                || unit.intent() == RequirementIntent.AUTHENTICATE
                || requiresAuthenticationSetup(unit)
                ? pageResolver.routeFor(RequirementCapability.AUTHENTICATION)
                : unit.route();
    }

    private AssertionType defaultAssertionType(RequirementUnit unit) {
        return switch (unit.intent()) {
            case VERIFY_ROUTE -> AssertionType.URL_CONTAINS;
            case VERIFY_AUTHENTICATED_AREA -> AssertionType.AUTHENTICATED_AREA_VISIBLE;
            case VERIFY_PAGE_ACCESSIBLE -> AssertionType.URL_CONTAINS;
            default -> AssertionType.ELEMENT_VISIBLE;
        };
    }

    private String defaultExpectedValue(RequirementUnit unit, AssertionType assertionType, String targetRoute) {
        if (assertionType == AssertionType.URL_CONTAINS) {
            return firstNonBlank(targetRoute, unit.route());
        }
        return firstNonBlank(unit.requirement().expectedResult(), unit.requirement().title(), unit.requirement().statement());
    }

    private AssertionIntentKind assertionIntent(AssertionType type, RequirementUnit unit) {
        if (type == AssertionType.URL_CONTAINS) {
            return AssertionIntentKind.URL_CONTAINS;
        }
        if (unit.intent() == RequirementIntent.VERIFY_PAGE_ACCESSIBLE) {
            return AssertionIntentKind.PAGE_ACCESSIBLE;
        }
        if (type == AssertionType.AUTHENTICATED_AREA_VISIBLE) {
            return AssertionIntentKind.SUCCESS_STATE_VISIBLE;
        }
        if (type == AssertionType.FORM_VISIBLE) {
            return AssertionIntentKind.PAGE_VISIBLE;
        }
        return AssertionIntentKind.CONTENT_VISIBLE;
    }

    private List<ScenarioStepCandidate> deduplicateSteps(List<ScenarioStepCandidate> steps) {
        return steps.stream()
                .collect(java.util.stream.Collectors.toMap(
                        step -> step.kind() + "|" + step.ownerPage() + "|" + step.route() + "|" + step.setup(),
                        step -> step,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new
                ))
                .values().stream()
                .toList();
    }

    private String entryDataKey(RequirementUnit unit) {
        String text = String.join(" ",
                safe(unit.requirement().title()),
                safe(unit.requirement().statement()),
                safe(unit.requirement().expectedResult())
        ).toLowerCase(java.util.Locale.ROOT);
        if (text.contains("username") || text.contains("user name") || text.contains("login")) {
            return "username";
        }
        if (text.contains("password") || text.contains("pass")) {
            return "password";
        }
        if (text.contains("email")) {
            return "email";
        }
        return "value";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
