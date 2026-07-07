package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;

import java.util.List;
import java.util.Locale;

public class PomNoFallbackLocatorRule implements PomContractRule {

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        validateLocators(spec.locators(), context, issues);
        for (PomComponentSpec component : spec.components()) {
            validateLocators(component.locators(), context, issues);
        }
    }

    private void validateLocators(
            List<PomLocatorSpec> locators,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        for (PomLocatorSpec locator : locators) {
            String evidence = (locator.evidenceType() + " " + String.join(" ", locator.sourceTrace()))
                    .toLowerCase(Locale.ROOT);
            if (evidence.contains("fallback_locator") || evidence.contains("fallback-locator")
                    || evidence.contains("candidate_locator") || evidence.contains("candidate-locator")) {
                issues.add(context.blocker("POM_NO_FALLBACK_LOCATOR",
                        "POM contract may use only confirmed locator evidence",
                        locator.id() + " -> " + locator.evidenceType()));
            }
        }
    }
}
