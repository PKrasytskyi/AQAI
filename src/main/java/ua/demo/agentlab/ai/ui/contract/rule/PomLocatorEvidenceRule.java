package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PomLocatorEvidenceRule implements PomContractRule {

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        validateLocators(spec.locators(), context, issues);
        for (PomComponentSpec component : spec.components()) {
            validateLocators(component.locators(), context, issues);
        }
    }

    public Set<String> validateLocators(
            List<PomLocatorSpec> locators,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        Set<String> ids = new LinkedHashSet<>();
        for (PomLocatorSpec locator : locators) {
            if (locator.id().isBlank()) {
                issues.add(context.blocker("POM_LOCATOR_ID_PRESENT", "Locator id is required", locator.toString()));
                continue;
            }
            if (!ids.add(locator.id())) {
                issues.add(context.blocker("POM_LOCATOR_ID_UNIQUE", "Locator ids must be unique", locator.id()));
            }
            if (!context.locatorStrategies().contains(locator.strategy())) {
                issues.add(context.blocker("POM_LOCATOR_STRATEGY_SUPPORTED",
                        "Locator strategy is not supported",
                        locator.id() + " -> " + locator.strategy()));
            }
            if (locator.value().isBlank()) {
                issues.add(context.blocker("POM_LOCATOR_VALUE_PRESENT", "Locator value is required", locator.id()));
            }
            if (locator.stabilityScore() > 0.0d && locator.stabilityScore() < 0.50d) {
                issues.add(context.warning("POM_LOCATOR_LOW_STABILITY",
                        "Locator stability is low",
                        locator.id() + " -> " + locator.stabilityScore()));
            }
            if ("FALLBACK_LOCATOR".equalsIgnoreCase(locator.evidenceType())) {
                issues.add(context.blocker("POM_LOCATOR_CONFIRMED_EVIDENCE",
                        "POM contract must not use fallback locators",
                        locator.id()));
            }
            if (!locator.sameOrigin()) {
                issues.add(context.blocker("POM_LOCATOR_SAME_ORIGIN",
                        "POM contract must not use external-origin locator evidence",
                        locator.id()));
            }
        }
        return ids;
    }
}
