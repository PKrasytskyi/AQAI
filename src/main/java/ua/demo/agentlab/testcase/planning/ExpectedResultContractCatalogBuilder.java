package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

class ExpectedResultContractCatalogBuilder {

    ExpectedResultContractCatalog build(List<RequirementUnit> units) {
        if (units == null || units.isEmpty()) {
            return new ExpectedResultContractCatalog(List.of());
        }
        List<ExpectedResultContract> contracts = units.stream()
                .filter(unit -> unit.type() == RequirementUnitType.ASSERTION
                        || unit.type() == RequirementUnitType.ROUTE_EXPECTATION
                        || hasStructuredExpectedResult(unit))
                .flatMap(this::toContracts)
                .toList();
        return new ExpectedResultContractCatalog(contracts);
    }

    private Stream<ExpectedResultContract> toContracts(RequirementUnit unit) {
        NormalizedRequirement requirement = unit.requirement();
        if (requirement.structuredAssertions() != null && !requirement.structuredAssertions().isEmpty()) {
            return requirement.structuredAssertions().stream().map(assertion -> {
                AssertionType assertionType = structuredAssertionType(assertion.type());
                return new ExpectedResultContract(
                        requirement.id(),
                        assertionType,
                        assertion.target(),
                        structuredExpectedValue(assertion.expectedValue(), assertionType, unit),
                        unit.ownerPage(),
                        unit.route(),
                        sourceReference(assertion.sourceReference()),
                        0.96d
                );
            });
        }
        String expectedValue = expectedValue(requirement, unit);
        return Stream.of(new ExpectedResultContract(
                requirement.id(),
                assertionType(unit, expectedValue),
                "",
                expectedValue,
                unit.ownerPage(),
                unit.route(),
                sourceReference(requirement.sourceReference()),
                unit.type() == RequirementUnitType.ROUTE_EXPECTATION ? 1.0d : 0.88d
        ));
    }

    private AssertionType structuredAssertionType(String value) {
        if (value == null || value.isBlank()) {
            return AssertionType.ELEMENT_VISIBLE;
        }
        try {
            return AssertionType.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return AssertionType.DATA_STATE_MATCHES;
        }
    }

    private String structuredExpectedValue(String expectedValue, AssertionType type, RequirementUnit unit) {
        String value = expectedValue == null ? "" : expectedValue.trim();
        if ((type == AssertionType.URL_CONTAINS || type == AssertionType.ROUTE_EQUALS
                || type == AssertionType.ROUTE_REACHED || type == AssertionType.ROUTE_CHANGED)
                && (!value.startsWith("/") || value.startsWith("//"))) {
            return unit.route();
        }
        return value;
    }

    private String expectedValue(NormalizedRequirement requirement, RequirementUnit unit) {
        if (unit.type() == RequirementUnitType.ROUTE_EXPECTATION || unit.intent() == RequirementIntent.VERIFY_ROUTE) {
            return unit.route();
        }
        String expected = firstNonBlank(requirement.expectedResult(), requirement.statement(), requirement.title());
        return expected;
    }

    private AssertionType assertionType(RequirementUnit unit, String expectedValue) {
        String text = normalize(expectedValue + " " + unit.requirement().title() + " " + unit.requirement().statement());
        if (unit.intent() == RequirementIntent.VERIFY_ROUTE || containsAny(text, "route", "url")) {
            return AssertionType.URL_CONTAINS;
        }
        if (containsAny(text, "login page", "form") && containsAny(text, "visible", "displays")) {
            return AssertionType.FORM_VISIBLE;
        }
        if (unit.capability() == RequirementCapability.AUTHENTICATED_AREA) {
            return AssertionType.AUTHENTICATED_AREA_VISIBLE;
        }
        if (unit.capability() == RequirementCapability.FILTER
                || unit.capability() == RequirementCapability.RESULTS_COLLECTION
                || containsAny(text, "results refresh", "matching all configured criteria", "matching vacancy", "displayed vacancy")) {
            return AssertionType.DATA_STATE_MATCHES;
        }
        if (containsAny(text, "visible", "displays", "accessible", "see")) {
            return AssertionType.ELEMENT_VISIBLE;
        }
        return AssertionType.TEXT_VISIBLE;
    }

    private boolean hasStructuredExpectedResult(RequirementUnit unit) {
        return unit != null && unit.requirement() != null
                && unit.requirement().tags() != null
                && unit.requirement().tags().contains("structured-requirement")
                && unit.requirement().expectedResult() != null
                && !unit.requirement().expectedResult().isBlank();
    }

    private String sourceReference(SourceReference reference) {
        if (reference == null) {
            return "";
        }
        return reference.startLine() > 0 ? reference.source() + " [L" + reference.startLine() + "]" : reference.source();
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(normalize(fragment))) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
