package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;

import java.util.List;
import java.util.Locale;

public class PomNoRequirementSentenceAssertionRule implements PomContractRule {

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
                if (looksLikeRequirementSentence(check)) {
                    issues.add(context.blocker("POM_NO_REQUIREMENT_SENTENCE_ASSERTION",
                            "POM assertion expectedValue must be concrete UI evidence, not a requirement sentence",
                            assertion.methodName() + " -> " + check.expectedValue()));
                }
            }
        }
    }

    private boolean looksLikeRequirementSentence(PomCheckSpec check) {
        if (check == null || check.expectedValue().isBlank()) {
            return false;
        }
        if (check.check() == PomCheckType.URL_CONTAINS || check.check() == PomCheckType.URL_EQUALS) {
            return false;
        }
        String value = check.expectedValue().trim();
        String normalized = value.toLowerCase(Locale.ROOT);
        return value.length() > 60
                || normalized.startsWith("user can ")
                || normalized.startsWith("user is ")
                || normalized.startsWith("home page ")
                || normalized.startsWith("login page ")
                || normalized.startsWith("authenticated area ")
                || normalized.endsWith(".");
    }
}
