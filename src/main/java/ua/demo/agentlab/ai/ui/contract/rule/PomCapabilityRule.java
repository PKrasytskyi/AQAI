package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;

import java.util.List;
import java.util.Set;

public class PomCapabilityRule implements PomContractRule {

    private static final Set<String> WEAK_CAPABILITIES = Set.of("", "UNKNOWN", "SEE PAGE CAPABILITY CONTRACT");

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        PomPageSpec page = spec.page();
        String capability = page == null ? "" : page.capability().trim().toUpperCase(java.util.Locale.ROOT);
        if (WEAK_CAPABILITIES.contains(capability)) {
            issues.add(context.blocker("POM_CAPABILITY_PRESENT",
                    "Page capability must be explicit",
                    page == null ? "" : page.toString()));
        }
    }
}
