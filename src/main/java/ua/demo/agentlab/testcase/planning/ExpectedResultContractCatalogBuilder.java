package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;

import java.util.List;
import java.util.Locale;

class ExpectedResultContractCatalogBuilder {

    ExpectedResultContractCatalog build(List<RequirementUnit> units) {
        if (units == null || units.isEmpty()) {
            return new ExpectedResultContractCatalog(List.of());
        }
        List<ExpectedResultContract> contracts = units.stream()
                .filter(unit -> unit.type() == RequirementUnitType.ASSERTION
                        || unit.type() == RequirementUnitType.ROUTE_EXPECTATION)
                .map(this::toContract)
                .toList();
        return new ExpectedResultContractCatalog(contracts);
    }

    private ExpectedResultContract toContract(RequirementUnit unit) {
        NormalizedRequirement requirement = unit.requirement();
        String expectedValue = expectedValue(requirement, unit);
        return new ExpectedResultContract(
                requirement.id(),
                assertionType(unit, expectedValue),
                expectedValue,
                unit.ownerPage(),
                unit.route(),
                sourceReference(requirement.sourceReference()),
                unit.type() == RequirementUnitType.ROUTE_EXPECTATION ? 1.0d : 0.88d
        );
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
        if (containsAny(text, "visible", "displays", "accessible", "see")) {
            return AssertionType.ELEMENT_VISIBLE;
        }
        return AssertionType.TEXT_VISIBLE;
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
