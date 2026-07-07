package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;

import java.util.List;
import java.util.Locale;

public class PomBusinessIntentRule implements PomContractRule {

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        String page = (spec.page().name() + " " + spec.page().capability()).toLowerCase(Locale.ROOT);
        if (!containsAny(page, "login", "authentication_form", "authentication-form")) {
            return;
        }
        spec.actions().stream()
                .filter(action -> action.methodName().equalsIgnoreCase("login"))
                .forEach(action -> validateLoginAction(action, context, issues));
    }

    private void validateLoginAction(
            PomActionSpec action,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        long typedSteps = action.steps().stream()
                .filter(step -> step.action() == PomStepAction.CLEAR_AND_TYPE || step.action() == PomStepAction.SEND_KEYS)
                .count();
        boolean clicks = action.steps().stream().anyMatch(step -> step.action() == PomStepAction.CLICK);
        String locators = action.steps().stream()
                .map(step -> step.locator().toLowerCase(Locale.ROOT))
                .reduce("", (left, right) -> left + " " + right);
        if (typedSteps < 2 || !clicks || !containsAny(locators, "username", "user") || !locators.contains("password")) {
            issues.add(context.blocker("POM_BUSINESS_INTENT_AUTHENTICATE_FLOW",
                    "Login action must type username, type password, and submit using owned locators",
                    action.methodName()));
        }
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
