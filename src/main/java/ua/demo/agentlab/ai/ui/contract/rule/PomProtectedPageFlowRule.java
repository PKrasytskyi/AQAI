package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;

import java.util.List;
import java.util.Locale;

public class PomProtectedPageFlowRule implements PomContractRule {

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        String page = (spec.page().name() + " " + spec.page().capability()).toLowerCase(Locale.ROOT);
        if (!containsAny(page, "dashboard", "secure", "authenticated", "protected")) {
            return;
        }
        String behavior = behaviorText(spec);
        boolean mentionsLogout = containsAny(behavior, "logout", "signout", "sign out");
        if (!mentionsLogout) {
            return;
        }
        boolean hasUserMenuAction = containsAny(behavior, "openusermenu", "userdropdown", "userdrop", "dropdown", "profilemenu");
        boolean hasGap = spec.coverageGaps().stream()
                .map(gap -> gap.toLowerCase(Locale.ROOT))
                .anyMatch(gap -> containsAny(gap, "dropdown", "user menu", "profile menu", "logout"));
        if (!hasUserMenuAction && !hasGap) {
            issues.add(context.blocker("POM_PROTECTED_PAGE_FLOW_PREREQUISITE",
                    "Protected page logout requires user menu/dropdown prerequisite action or explicit coverage gap",
                    spec.page().name()));
        }
    }

    private String behaviorText(PomContractSpec spec) {
        StringBuilder builder = new StringBuilder();
        appendActions(builder, spec.actions());
        appendAssertions(builder, spec.assertions());
        for (PomComponentSpec component : spec.components()) {
            builder.append(' ').append(component.name()).append(' ').append(component.type()).append(' ');
            appendActions(builder, component.actions());
            appendAssertions(builder, component.assertions());
        }
        spec.locators().forEach(locator -> builder.append(' ')
                .append(locator.id()).append(' ')
                .append(locator.elementName()).append(' ')
                .append(locator.value()).append(' '));
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private void appendActions(StringBuilder builder, List<PomActionSpec> actions) {
        actions.forEach(action -> builder.append(' ').append(action.methodName()).append(' '));
    }

    private void appendAssertions(StringBuilder builder, List<PomAssertionSpec> assertions) {
        assertions.forEach(assertion -> builder.append(' ').append(assertion.methodName()).append(' '));
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
