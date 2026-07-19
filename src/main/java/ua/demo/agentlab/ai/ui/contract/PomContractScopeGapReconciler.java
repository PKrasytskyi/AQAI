package ua.demo.agentlab.ai.ui.contract;

import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyAssertion;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reconciles deterministic scope facts that must not depend on the model copying typed evidence verbatim.
 */
public class PomContractScopeGapReconciler {

    public PomContractSpec reconcile(PomContractSpec contract, PromptReadyPomScope scope) {
        if (contract == null || scope == null) {
            return contract;
        }

        LinkedHashSet<String> coverageGaps = new LinkedHashSet<>();
        addNonBlank(coverageGaps, contract.coverageGaps());
        addNonBlank(coverageGaps, scope.coverageGaps());
        List<PomAssertionSpec> assertions = reconcileAssertions(contract, scope);

        return new PomContractSpec(
                contract.schemaVersion(),
                contract.page(),
                contract.locators(),
                contract.components(),
                contract.actions(),
                assertions,
                List.copyOf(coverageGaps),
                contract.rejectedSuggestions()
        );
    }

    private List<PomAssertionSpec> reconcileAssertions(PomContractSpec contract, PromptReadyPomScope scope) {
        List<PomAssertionSpec> assertions = new ArrayList<>(contract.assertions());
        Set<String> allowedLocators = scope.allowedLocators().stream()
                .map(locator -> normalize(locator.id()))
                .collect(java.util.stream.Collectors.toSet());
        for (PromptReadyAssertion required : scope.ownedAssertions()) {
            if (covered(contract, assertions, required)) {
                continue;
            }
            PomAssertionSpec deterministic = deterministicAssertion(required, allowedLocators);
            if (deterministic != null) {
                assertions.add(deterministic);
            }
        }
        return List.copyOf(assertions);
    }

    private boolean covered(
            PomContractSpec contract,
            List<PomAssertionSpec> pageAssertions,
            PromptReadyAssertion required
    ) {
        List<PomAssertionSpec> all = new ArrayList<>(pageAssertions);
        contract.components().forEach(component -> all.addAll(component.assertions()));
        return all.stream().flatMap(assertion -> assertion.checks().stream())
                .anyMatch(check -> matches(required, check));
    }

    private boolean matches(PromptReadyAssertion required, PomCheckSpec check) {
        String type = required.type().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "ELEMENT_VISIBLE" -> check.check() == PomCheckType.VISIBLE
                    && same(required.targetLocatorId(), check.locator());
            case "URL_CONTAINS" -> check.check() == PomCheckType.URL_CONTAINS
                    && same(required.expectedValue(), firstNonBlank(check.route(), check.expectedValue()));
            case "URL_EQUALS", "ROUTE_EQUALS" -> check.check() == PomCheckType.URL_EQUALS
                    && same(required.expectedValue(), firstNonBlank(check.route(), check.expectedValue()));
            default -> false;
        };
    }

    private PomAssertionSpec deterministicAssertion(PromptReadyAssertion required, Set<String> allowedLocators) {
        String type = required.type().toUpperCase(Locale.ROOT);
        if ("ELEMENT_VISIBLE".equals(type)
                && !required.targetLocatorId().isBlank()
                && allowedLocators.contains(normalize(required.targetLocatorId()))) {
            String locator = required.targetLocatorId();
            return new PomAssertionSpec(
                    "is" + upperCamel(locator) + "Visible",
                    "boolean",
                    List.of(new PomCheckSpec(PomCheckType.VISIBLE, locator, locator, "", "", "")),
                    "AND"
            );
        }
        if (("URL_CONTAINS".equals(type) || "URL_EQUALS".equals(type) || "ROUTE_EQUALS".equals(type))
                && !required.expectedValue().isBlank()) {
            PomCheckType check = "URL_CONTAINS".equals(type) ? PomCheckType.URL_CONTAINS : PomCheckType.URL_EQUALS;
            return new PomAssertionSpec(
                    "isOn" + upperCamel(required.ownerPage()),
                    "boolean",
                    List.of(new PomCheckSpec(check, "", required.expectedValue(), "", "", required.expectedValue())),
                    "AND"
            );
        }
        return null;
    }

    private String upperCamel(String value) {
        String[] tokens = (value == null ? "" : value).split("[^A-Za-z0-9]+");
        StringBuilder result = new StringBuilder();
        for (String token : tokens) {
            if (!token.isBlank()) {
                result.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
            }
        }
        return result.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean same(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second == null ? "" : second : first;
    }

    private void addNonBlank(LinkedHashSet<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(target::add);
    }
}
