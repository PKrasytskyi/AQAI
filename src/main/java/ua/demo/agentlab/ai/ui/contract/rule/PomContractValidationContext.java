package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSeverity;

import java.util.Set;

public class PomContractValidationContext {

    private final Set<String> locatorStrategies;
    private final Set<String> allowedReturnTypes;

    public PomContractValidationContext(Set<String> locatorStrategies, Set<String> allowedReturnTypes) {
        this.locatorStrategies = locatorStrategies == null ? Set.of() : Set.copyOf(locatorStrategies);
        this.allowedReturnTypes = allowedReturnTypes == null ? Set.of() : Set.copyOf(allowedReturnTypes);
    }

    public Set<String> locatorStrategies() {
        return locatorStrategies;
    }

    public Set<String> allowedReturnTypes() {
        return allowedReturnTypes;
    }

    public PomContractIssue blocker(String ruleId, String message, String evidence) {
        return new PomContractIssue(PomContractSeverity.BLOCKER, ruleId, message, evidence);
    }

    public PomContractIssue warning(String ruleId, String message, String evidence) {
        return new PomContractIssue(PomContractSeverity.WARNING, ruleId, message, evidence);
    }
}
