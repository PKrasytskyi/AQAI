package ua.demo.agentlab.ai.ui.contract;

import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyAssertion;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Validates that an LLM contract stays inside the prompt's page-owned evidence boundary. */
public class PomContractScopeValidator {

    public PomContractQualityReport validate(PomContractSpec contract, PromptReadyPomScope scope) {
        if (contract == null || scope == null) {
            return new PomContractQualityReport("pom-contract-v1", "", List.of(
                    blocker("POM_SCOPE_PRESENT", "POM contract and prompt scope are required", "missing contract or scope")
            ));
        }
        List<PomContractIssue> issues = new ArrayList<>();
        Set<String> declaredActions = scope.ownedActions().stream()
                .map(this::methodName)
                .filter(name -> !name.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> allowedLocators = new LinkedHashSet<>(scope.allowedLocators().stream()
                .map(locator -> locator.id().toLowerCase(Locale.ROOT))
                .toList());
        Set<String> referencedLocators = referencedLocators(contract);

        for (PomActionSpec action : contract.actions()) {
            String name = action.methodName();
            if (!declaredActions.contains(name)) {
                issues.add(blocker("POM_SCOPE_ACTION_DECLARED",
                        "Action must be declared by the page-owned contract", name));
            }
            if (action.steps().isEmpty()) {
                issues.add(blocker("POM_SCOPE_ACTION_STEPS_PRESENT",
                        "Action without deterministic steps must be a coverage gap, not an action", name));
            }
        }
        for (PomLocatorSpec locator : allLocators(contract)) {
            String locatorId = locator.id().toLowerCase(Locale.ROOT);
            if (!allowedLocators.contains(locatorId)) {
                issues.add(blocker("POM_SCOPE_LOCATOR_ALLOWED",
                        "Locator is not part of the selected confirmed prompt evidence", locator.id()));
            }
            if (!referencedLocators.contains(locatorId)) {
                issues.add(blocker("POM_SCOPE_LOCATOR_RELEVANT",
                        "Declared locator must be referenced by an action, assertion, or component root", locator.id()));
            }
        }
        for (PromptReadyAssertion required : scope.ownedAssertions()) {
            if (!coversAssertion(contract, required) && !hasCoverageGap(contract, required)) {
                issues.add(blocker("POM_SCOPE_ASSERTION_COVERED",
                        "Required assertion must be implemented or represented as a coverage gap",
                        required.type() + "(" + required.expectedValue() + ")"));
            }
        }
        for (String requiredGap : scope.coverageGaps()) {
            if (!hasCoverageGap(contract, requiredGap)) {
                issues.add(blocker("POM_SCOPE_COVERAGE_GAP_REPRESENTED",
                        "Required scope coverage gap must be retained by the POM contract",
                        requiredGap));
            }
        }
        return new PomContractQualityReport(contract.schemaVersion(), contract.page().name(), issues);
    }

    private Set<String> referencedLocators(PomContractSpec contract) {
        Set<String> ids = new LinkedHashSet<>();
        for (PomActionSpec action : contract.actions()) {
            action.steps().forEach(step -> add(ids, step.locator()));
        }
        for (PomAssertionSpec assertion : contract.assertions()) {
            assertion.checks().forEach(check -> add(ids, check.locator()));
        }
        for (PomComponentSpec component : contract.components()) {
            add(ids, component.rootLocatorId());
            component.actions().forEach(action -> action.steps().forEach(step -> add(ids, step.locator())));
            component.assertions().forEach(assertion -> assertion.checks().forEach(check -> add(ids, check.locator())));
        }
        return ids;
    }

    private List<PomLocatorSpec> allLocators(PomContractSpec contract) {
        List<PomLocatorSpec> locators = new ArrayList<>(contract.locators());
        contract.components().forEach(component -> locators.addAll(component.locators()));
        return locators;
    }

    private boolean coversAssertion(PomContractSpec contract, PromptReadyAssertion required) {
        String type = required.type().toUpperCase(Locale.ROOT);
        String expected = required.expectedValue();
        if ("FORM_VISIBLE".equals(type)) {
            return contract.assertions().stream().anyMatch(assertion -> assertion.checks().stream()
                    .filter(check -> check.check() == PomCheckType.VISIBLE)
                    .map(check -> check.locator().toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toSet()).size() >= 2);
        }
        return contract.assertions().stream().flatMap(assertion -> assertion.checks().stream())
                .anyMatch(check -> matches(required, check, expected));
    }

    private boolean matches(PromptReadyAssertion required, PomCheckSpec check, String expected) {
        String type = required.type().toUpperCase(Locale.ROOT);
        if ("URL_CONTAINS".equals(type)) {
            return check.check() == PomCheckType.URL_CONTAINS
                    && same(expected, firstNonBlank(check.route(), check.expectedValue()));
        }
        if ("URL_EQUALS".equals(type) || "ROUTE_EQUALS".equals(type)) {
            return check.check() == PomCheckType.URL_EQUALS
                    && same(expected, firstNonBlank(check.route(), check.expectedValue()));
        }
        if ("ELEMENT_VISIBLE".equals(type)) {
            return check.check() == PomCheckType.VISIBLE
                    && !required.targetLocatorId().isBlank()
                    && same(required.targetLocatorId(), check.locator());
        }
        return check.check().name().equals(type)
                && (expected.isBlank() || same(expected, check.expectedValue()) || same(expected, check.locator()));
    }

    private boolean hasCoverageGap(PomContractSpec contract, PromptReadyAssertion required) {
        String expected = (required.type() + " " + required.expectedValue()).toLowerCase(Locale.ROOT);
        return contract.coverageGaps().stream()
                .map(value -> value == null ? "" : value.toLowerCase(Locale.ROOT))
                .anyMatch(gap -> gap.contains(required.expectedValue().toLowerCase(Locale.ROOT))
                        || gap.contains(expected));
    }

    private boolean hasCoverageGap(PomContractSpec contract, String requiredGap) {
        String normalizedRequired = requiredGap == null ? "" : requiredGap.trim().toLowerCase(Locale.ROOT);
        return !normalizedRequired.isBlank() && contract.coverageGaps().stream()
                .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(gap -> gap.equals(normalizedRequired));
    }

    private void add(Set<String> target, String value) {
        if (value != null && !value.isBlank()) target.add(value.trim().toLowerCase(Locale.ROOT));
    }

    private String methodName(String signature) {
        if (signature == null) return "";
        int index = signature.indexOf('(');
        return (index < 0 ? signature : signature.substring(0, index)).trim();
    }

    private boolean same(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second == null ? "" : second : first;
    }

    private PomContractIssue blocker(String rule, String message, String evidence) {
        return new PomContractIssue(PomContractSeverity.BLOCKER, rule, message, evidence);
    }
}
