package ua.demo.agentlab.ai.ui.contract;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Removes schema-level duplication without relaxing semantic scope validation. */
public final class PomContractStructuralNormalizer {

    public PomContractSpec normalize(PomContractSpec contract) {
        if (contract == null) {
            return null;
        }
        PomPageSpec page = new PomPageSpec(contract.page().name(), contract.page().route(),
                contract.page().capability(), normalizeMethodName(contract.page().openMethod()));
        String openMethod = page.openMethod();
        List<PomActionSpec> actions = contract.actions().stream()
                .map(this::normalizeAction)
                .filter(action -> !isRedundantPageOpen(action, openMethod, contract.page().route()))
                .toList();
        actions = normalizeDependentActionSequences(actions);
        List<PomComponentSpec> components = contract.components().stream()
                .map(component -> normalize(component, openMethod, contract.page().route()))
                .toList();
        return new PomContractSpec(contract.schemaVersion(), page, contract.locators(), components,
                actions, normalizeAssertions(contract.assertions()), contract.coverageGaps(),
                contract.rejectedSuggestions());
    }

    private PomComponentSpec normalize(PomComponentSpec component, String openMethod, String route) {
        List<PomActionSpec> actions = component.actions().stream()
                .map(this::normalizeAction)
                .filter(action -> !isRedundantPageOpen(action, openMethod, route))
                .toList();
        actions = normalizeDependentActionSequences(actions);
        return new PomComponentSpec(component.name(), component.type(), component.rootLocatorId(),
                component.locators(), actions, normalizeAssertions(component.assertions()), component.reusable());
    }

    private List<PomAssertionSpec> normalizeAssertions(List<PomAssertionSpec> assertions) {
        return assertions.stream()
                .map(assertion -> {
                    List<PomCheckSpec> checks = assertion.checks().stream().map(this::normalizeCheck).toList();
                    return new PomAssertionSpec(assertionMethodName(assertion.methodName(), checks),
                            assertion.returnType(), checks, assertion.combine());
                })
                .toList();
    }

    private String assertionMethodName(String suggestedName, List<PomCheckSpec> checks) {
        if (checks.size() != 1 || !generatedAssertionName(suggestedName)) {
            return normalizeMethodName(suggestedName);
        }
        PomCheckSpec check = checks.get(0);
        String subject = switch (check.check()) {
            case URL_CONTAINS, URL_EQUALS -> firstNonBlank(check.route(), check.expectedValue());
            default -> check.locator();
        };
        return normalizeMethodName(check.check().name() + "_" + subject);
    }

    private boolean generatedAssertionName(String name) {
        String normalized = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
        return java.util.Arrays.stream(PomCheckType.values())
                .anyMatch(type -> normalized.startsWith(type.name() + "_") || normalized.equals(type.name()));
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second == null ? "" : second;
    }

    private PomActionSpec normalizeAction(PomActionSpec action) {
        return new PomActionSpec(normalizeMethodName(action.methodName()), action.kind(),
                action.parameters(), action.steps());
    }

    /**
     * Preserves a confirmed prerequisite when the model splits a composite UI action into public helper methods.
     * The normalizer composes only steps already present in the contract; it never invents a locator or action.
     */
    private List<PomActionSpec> normalizeDependentActionSequences(List<PomActionSpec> actions) {
        PomActionSpec menuAction = actions.stream()
                .filter(this::isMenuOpenAction)
                .filter(action -> !action.steps().isEmpty())
                .filter(action -> action.steps().stream().allMatch(step -> step.action() == PomStepAction.CLICK))
                .findFirst()
                .orElse(null);
        if (menuAction == null) {
            return actions;
        }
        Set<String> menuLocators = menuAction.steps().stream()
                .map(PomStepSpec::locator)
                .filter(locator -> locator != null && !locator.isBlank())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        if (menuLocators.isEmpty()) {
            return actions;
        }
        return actions.stream()
                .map(action -> prependMenuPrerequisite(action, menuAction.steps(), menuLocators))
                .toList();
    }

    private PomActionSpec prependMenuPrerequisite(
            PomActionSpec action,
            List<PomStepSpec> menuSteps,
            Set<String> menuLocators
    ) {
        if (!isLogoutAction(action) || action.steps().isEmpty()) {
            return action;
        }
        boolean alreadyOpensMenu = action.steps().stream()
                .filter(step -> step.action() == PomStepAction.CLICK)
                .map(PomStepSpec::locator)
                .anyMatch(menuLocators::contains);
        boolean clicksLogout = action.steps().stream()
                .filter(step -> step.action() == PomStepAction.CLICK)
                .map(PomStepSpec::locator)
                .map(this::normalized)
                .anyMatch(locator -> locator.contains("logout") || locator.contains("signout"));
        if (alreadyOpensMenu || !clicksLogout) {
            return action;
        }
        java.util.ArrayList<PomStepSpec> steps = new java.util.ArrayList<>(menuSteps);
        steps.addAll(action.steps());
        return new PomActionSpec(action.methodName(), action.kind(), action.parameters(), steps);
    }

    private boolean isMenuOpenAction(PomActionSpec action) {
        String method = normalized(action.methodName());
        return method.contains("open")
                && (method.contains("usermenu")
                || method.contains("userdropdown")
                || method.contains("profilemenu")
                || method.contains("profiledropdown"));
    }

    private boolean isLogoutAction(PomActionSpec action) {
        String method = normalized(action.methodName());
        return method.contains("logout") || method.contains("signout");
    }

    private String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private PomCheckSpec normalizeCheck(PomCheckSpec check) {
        if (check.check() != PomCheckType.VISIBLE || check.locator().isBlank()) {
            return check;
        }
        return new PomCheckSpec(check.check(), check.locator(), check.locator(), check.valueFrom(),
                check.attribute(), check.route());
    }

    private String normalizeMethodName(String value) {
        String[] tokens = (value == null ? "" : value).split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) continue;
            String normalized = allUpperCase(token) ? token.toLowerCase(Locale.ROOT) : token;
            builder.append(Character.toUpperCase(normalized.charAt(0)));
            if (normalized.length() > 1) builder.append(normalized.substring(1));
        }
        if (builder.isEmpty()) return "";
        return Character.toLowerCase(builder.charAt(0)) + builder.substring(1);
    }

    private boolean allUpperCase(String value) {
        return value.chars().anyMatch(Character::isLetter)
                && value.equals(value.toUpperCase(Locale.ROOT));
    }

    private boolean isRedundantPageOpen(PomActionSpec action, String openMethod, String route) {
        if (action == null || openMethod == null || !action.methodName().equals(openMethod)
                || action.steps().isEmpty()) {
            return false;
        }
        return action.steps().stream().allMatch(step -> step.action() == PomStepAction.OPEN_ROUTE
                && (route == null || route.isBlank() || step.route().isBlank() || route.equals(step.route())));
    }
}
