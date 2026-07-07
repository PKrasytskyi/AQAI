package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;

import java.util.List;

public class PomLocatorProvenanceRule implements PomContractRule {

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
            if (locator.evidenceType().isBlank()) {
                issues.add(context.warning("POM_LOCATOR_PROVENANCE_PRESENT",
                        "Locator provenance is missing; deterministic rehydration should provide evidenceType",
                        locator.id()));
            }
            if (!locator.evidenceType().isBlank()
                    && !"CONFIRMED_LOCATOR".equalsIgnoreCase(locator.evidenceType())) {
                issues.add(context.blocker("POM_LOCATOR_PROVENANCE_CONFIRMED",
                        "Locator evidenceType must be CONFIRMED_LOCATOR",
                        locator.id() + " -> " + locator.evidenceType()));
            }
            if (!locator.sameOrigin()) {
                issues.add(context.blocker("POM_LOCATOR_PROVENANCE_SAME_ORIGIN",
                        "Locator provenance must be same-origin",
                        locator.id()));
            }
        }
    }
}
