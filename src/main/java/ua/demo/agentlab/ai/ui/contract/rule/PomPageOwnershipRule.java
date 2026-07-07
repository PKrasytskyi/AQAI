package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;

import java.util.List;
import java.util.Locale;

public class PomPageOwnershipRule implements PomContractRule {

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        String page = spec.page().name().toLowerCase(Locale.ROOT);
        String route = spec.page().route().toLowerCase(Locale.ROOT);
        if (page.contains("login") && containsAny(route, "dashboard", "secure")) {
            issues.add(context.blocker("POM_PAGE_OWNERSHIP_ROUTE",
                    "LoginPage must not own authenticated/protected route",
                    spec.page().name() + " -> " + spec.page().route()));
        }
        if (containsAny(page, "dashboard", "secure") && containsAny(route, "login", "auth/login")) {
            issues.add(context.blocker("POM_PAGE_OWNERSHIP_ROUTE",
                    "Protected page must not own login route",
                    spec.page().name() + " -> " + spec.page().route()));
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
