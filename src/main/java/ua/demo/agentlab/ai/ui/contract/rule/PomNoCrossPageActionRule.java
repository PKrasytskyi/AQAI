package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;

import java.util.List;
import java.util.Locale;

public class PomNoCrossPageActionRule implements PomContractRule {

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        String page = (spec.page().name() + " " + spec.page().capability()).toLowerCase(Locale.ROOT);
        validateNames(page, spec.actions(), spec.assertions(), context, issues);
        for (PomComponentSpec component : spec.components()) {
            validateNames(page, component.actions(), component.assertions(), context, issues);
        }
    }

    private void validateNames(
            String page,
            List<PomActionSpec> actions,
            List<PomAssertionSpec> assertions,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        for (PomActionSpec action : actions) {
            validateName(page, action.methodName(), context, issues);
        }
        for (PomAssertionSpec assertion : assertions) {
            validateName(page, assertion.methodName(), context, issues);
        }
    }

    private void validateName(
            String page,
            String methodName,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        String method = methodName == null ? "" : methodName.toLowerCase(Locale.ROOT);
        if (page.contains("login") || page.contains("authentication_form") || page.contains("authentication-form")) {
            if (containsAny(method, "logout", "dashboard", "securearea", "authenticatedarea")) {
                issues.add(context.blocker("POM_NO_CROSS_PAGE_ACTION",
                        "Login/authentication page must not own post-login or logout behavior",
                        methodName));
            }
        }
        if (containsAny(page, "dashboard", "secure", "authenticated")) {
            if (containsAny(method, "enterusername", "enterpassword", "clicklogin", "loginform")) {
                issues.add(context.blocker("POM_NO_CROSS_PAGE_ACTION",
                        "Protected page must not own login form behavior",
                        methodName));
            }
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
