package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;
import ua.demo.agentlab.ai.ui.contract.PomStepSpec;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        validateUserMenuTriggerLocators(spec, context, issues);
        if (!logoutActionHasMenuPrerequisite(spec) && !hasGap) {
            issues.add(context.blocker("POM_PROTECTED_PAGE_LOGOUT_SEQUENCE",
                    "Protected page logout action must click the user menu/dropdown trigger before clicking logout",
                    spec.page().name()));
        }
    }

    private void validateUserMenuTriggerLocators(
            PomContractSpec spec,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        for (PomLocatorSpec locator : allLocators(spec)) {
            if (!isUserMenuLocator(locator)) {
                continue;
            }
            String value = normalize(locator.value());
            if (containsAny(value,
                    "oxd-userdropdown-link",
                    "a[href",
                    "href=",
                    "auth/logout",
                    "help/support",
                    "updatepassword")) {
                issues.add(context.blocker("POM_PROTECTED_PAGE_MENU_TRIGGER_LOCATOR",
                        "User menu trigger must point to the dropdown opener, not a dropdown menu item",
                        locator.id() + "=" + locator.value()));
            }
        }
    }

    private boolean logoutActionHasMenuPrerequisite(PomContractSpec spec) {
        Set<String> userMenuLocators = new LinkedHashSet<>();
        Set<String> logoutLocators = new LinkedHashSet<>();
        for (PomLocatorSpec locator : allLocators(spec)) {
            if (isUserMenuLocator(locator)) {
                userMenuLocators.add(locator.id());
            }
            if (isLogoutLocator(locator)) {
                logoutLocators.add(locator.id());
            }
        }
        return allActions(spec).stream()
                .filter(action -> normalize(action.methodName()).contains("logout"))
                .anyMatch(action -> actionHasMenuBeforeLogout(action, userMenuLocators, logoutLocators));
    }

    private boolean actionHasMenuBeforeLogout(
            PomActionSpec action,
            Set<String> userMenuLocators,
            Set<String> logoutLocators
    ) {
        boolean menuClicked = false;
        boolean logoutClicked = false;
        for (PomStepSpec step : action.steps()) {
            if (step.action() != PomStepAction.CLICK) {
                continue;
            }
            String locator = step.locator();
            if (userMenuLocators.contains(locator) || containsAny(normalize(locator), "usermenu", "dropdown", "profilemenu")) {
                menuClicked = true;
            }
            if (logoutLocators.contains(locator) || normalize(locator).contains("logout")) {
                logoutClicked = true;
                if (!menuClicked) {
                    return false;
                }
            }
        }
        return logoutClicked && menuClicked;
    }

    private List<PomActionSpec> allActions(PomContractSpec spec) {
        java.util.ArrayList<PomActionSpec> actions = new java.util.ArrayList<>(spec.actions());
        for (PomComponentSpec component : spec.components()) {
            actions.addAll(component.actions());
        }
        return actions;
    }

    private List<PomLocatorSpec> allLocators(PomContractSpec spec) {
        java.util.ArrayList<PomLocatorSpec> locators = new java.util.ArrayList<>(spec.locators());
        for (PomComponentSpec component : spec.components()) {
            locators.addAll(component.locators());
        }
        return locators;
    }

    private boolean isUserMenuLocator(PomLocatorSpec locator) {
        String evidence = normalize(locator.id() + " " + locator.elementName() + " " + locator.role() + " " + locator.value());
        return containsAny(evidence, "usermenu", "user menu", "userdropdown", "dropdown tab", "dropdown-tab", "profilemenu");
    }

    private boolean isLogoutLocator(PomLocatorSpec locator) {
        String evidence = normalize(locator.id() + " " + locator.elementName() + " " + locator.value());
        return containsAny(evidence, "logout", "log out", "signout", "sign out");
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

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
