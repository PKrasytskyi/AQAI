package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;

import java.util.List;

public class PomExpectedValueRule implements PomContractRule {

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        validateAssertions(spec.assertions(), context, issues);
        for (PomComponentSpec component : spec.components()) {
            validateAssertions(component.assertions(), context, issues);
        }
    }

    private void validateAssertions(
            List<PomAssertionSpec> assertions,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        for (PomAssertionSpec assertion : assertions) {
            for (PomCheckSpec check : assertion.checks()) {
                if ((check.check() == PomCheckType.TEXT_CONTAINS || check.check() == PomCheckType.TEXT_EQUALS)
                        && looksLikeRequirementSentence(check.expectedValue())) {
                    issues.add(context.warning("POM_EXPECTED_VALUE_SHOULD_BE_ATOMIC",
                            "Expected text should be a concrete value, not a requirement sentence",
                            assertion.methodName() + " -> " + check.expectedValue()));
                }
            }
        }
    }

    private boolean looksLikeRequirementSentence(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("user can ")
                || normalized.startsWith("user should ")
                || normalized.startsWith("the user can ")
                || normalized.endsWith(" is visible.")
                || normalized.endsWith(" is accessible.");
    }
}
