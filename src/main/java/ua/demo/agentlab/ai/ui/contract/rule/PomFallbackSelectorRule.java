package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;

import java.util.List;

public class PomFallbackSelectorRule implements PomContractRule {

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        validate(spec.locators(), context, issues);
        for (PomComponentSpec component : spec.components()) {
            validate(component.locators(), context, issues);
        }
    }

    private void validate(
            List<PomLocatorSpec> locators,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        for (PomLocatorSpec locator : locators) {
            String value = locator.value().toLowerCase(java.util.Locale.ROOT);
            if (value.equals("body") || value.equals("html") || value.matches("^(div|span|button|input)$")
                    || value.contains(" > div > div")
                    || value.contains(":nth-child(")
                    || value.startsWith("/html/")
                    || value.startsWith("//*[@id='root']/")
                    || value.startsWith("//*[@id=\"root\"]/")) {
                issues.add(context.blocker("POM_LOCATOR_NO_GENERIC_FALLBACK_SELECTOR",
                        "POM contract must not use generic fallback selectors",
                        locator.id() + " -> " + locator.value()));
            }
        }
    }
}
