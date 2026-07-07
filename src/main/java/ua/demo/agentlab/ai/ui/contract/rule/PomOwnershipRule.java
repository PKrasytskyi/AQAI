package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;
import ua.demo.agentlab.ai.ui.contract.PomStepSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PomOwnershipRule implements PomContractRule {

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        Set<String> pageLocatorIds = new PomLocatorEvidenceRule().validateLocators(spec.locators(), context, issues);
        validateActions(spec.actions(), pageLocatorIds, context, issues);
        validateAssertions(spec.assertions(), pageLocatorIds, context, issues);
        validateComponents(spec.components(), pageLocatorIds, context, issues);
        boolean hasComponentBehavior = spec.components().stream()
                .anyMatch(component -> !component.actions().isEmpty() || !component.assertions().isEmpty());
        if (spec.actions().isEmpty() && spec.assertions().isEmpty()
                && !hasComponentBehavior && spec.coverageGaps().isEmpty()) {
            issues.add(context.blocker("POM_CONTRACT_HAS_BEHAVIOR_OR_GAPS",
                    "POM contract must contain actions, assertions, or explicit coverage gaps",
                    spec.page().name()));
        }
    }

    private void validateComponents(
            List<PomComponentSpec> components,
            Set<String> pageLocatorIds,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        Set<String> componentNames = new LinkedHashSet<>();
        for (PomComponentSpec component : components) {
            if (component.name().isBlank()) {
                issues.add(context.blocker("POM_COMPONENT_NAME_PRESENT", "Component name is required", component.toString()));
                continue;
            }
            if (!componentNames.add(component.name())) {
                issues.add(context.blocker("POM_COMPONENT_NAME_UNIQUE", "Component names must be unique", component.name()));
            }
            Set<String> componentLocatorIds = new PomLocatorEvidenceRule().validateLocators(component.locators(), context, issues);
            Set<String> allowedIds = new LinkedHashSet<>(componentLocatorIds);
            allowedIds.addAll(pageLocatorIds);
            if (!component.rootLocatorId().isBlank()
                    && !pageLocatorIds.contains(component.rootLocatorId())
                    && !componentLocatorIds.contains(component.rootLocatorId())) {
                issues.add(context.blocker("POM_COMPONENT_ROOT_LOCATOR_EXISTS",
                        "Component rootLocatorId must reference a page or component locator",
                        component.name() + " -> " + component.rootLocatorId()));
            }
            validateActions(component.actions(), allowedIds, context, issues);
            validateAssertions(component.assertions(), allowedIds, context, issues);
        }
    }

    private void validateActions(
            List<PomActionSpec> actions,
            Set<String> locatorIds,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        Set<String> methodNames = new LinkedHashSet<>();
        for (PomActionSpec action : actions) {
            if (action.methodName().isBlank()) {
                issues.add(context.blocker("POM_ACTION_METHOD_PRESENT", "Action method name is required", action.toString()));
            }
            if (!"ACTION".equals(action.kind())) {
                issues.add(context.blocker("POM_ACTION_KIND_VALID", "Action contract kind must be ACTION", action.toString()));
            }
            if (!methodNames.add(action.methodName())) {
                issues.add(context.blocker("POM_ACTION_METHOD_UNIQUE", "Action method names must be unique", action.methodName()));
            }
            if (action.steps().isEmpty()) {
                issues.add(context.blocker("POM_ACTION_STEPS_PRESENT", "Action method requires deterministic steps", action.methodName()));
            }
            validateParameters(action.methodName(), action.parameters(), context, issues);
            for (PomStepSpec step : action.steps()) {
                validateStep(action.methodName(), step, locatorIds, context, issues);
            }
        }
    }

    private void validateParameters(
            String methodName,
            List<AiMethodParameterSpec> parameters,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        Set<String> names = new LinkedHashSet<>();
        for (AiMethodParameterSpec parameter : parameters) {
            if (parameter.type().isBlank() || parameter.name().isBlank()) {
                issues.add(context.blocker("POM_ACTION_PARAMETER_COMPLETE",
                        "Action parameters require type and name",
                        methodName + " -> " + parameter));
            }
            if (!names.add(parameter.name())) {
                issues.add(context.blocker("POM_ACTION_PARAMETER_UNIQUE",
                        "Action parameter names must be unique",
                        methodName + " -> " + parameter.name()));
            }
        }
    }

    private void validateStep(
            String methodName,
            PomStepSpec step,
            Set<String> locatorIds,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        if (step.action() == PomStepAction.OPEN_ROUTE) {
            if (step.route().isBlank()) {
                issues.add(context.blocker("POM_STEP_ROUTE_PRESENT", "OPEN_ROUTE requires route", methodName));
            }
            return;
        }
        if (step.locator().isBlank() || !locatorIds.contains(step.locator())) {
            issues.add(context.blocker("POM_STEP_LOCATOR_EXISTS",
                    "Step must reference an approved locator id",
                    methodName + " -> " + step));
        }
        if (Set.of(PomStepAction.CLEAR_AND_TYPE, PomStepAction.SEND_KEYS, PomStepAction.SELECT_BY_VISIBLE_TEXT,
                PomStepAction.UPLOAD_FILE).contains(step.action())
                && step.valueFrom().isBlank()
                && step.literalValue().isBlank()) {
            issues.add(context.blocker("POM_STEP_VALUE_PRESENT",
                    "Input/select/upload steps require valueFrom or literalValue",
                    methodName + " -> " + step));
        }
    }

    private void validateAssertions(
            List<PomAssertionSpec> assertions,
            Set<String> locatorIds,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        Set<String> methodNames = new LinkedHashSet<>();
        for (PomAssertionSpec assertion : assertions) {
            if (assertion.methodName().isBlank()) {
                issues.add(context.blocker("POM_ASSERTION_METHOD_PRESENT", "Assertion method name is required", assertion.toString()));
            }
            if (!methodNames.add(assertion.methodName())) {
                issues.add(context.blocker("POM_ASSERTION_METHOD_UNIQUE",
                        "Assertion method names must be unique",
                        assertion.methodName()));
            }
            if (!context.allowedReturnTypes().contains(assertion.returnType())) {
                issues.add(context.blocker("POM_ASSERTION_RETURN_TYPE_SUPPORTED",
                        "Assertion return type is not supported",
                        assertion.methodName() + " -> " + assertion.returnType()));
            }
            if (assertion.checks().isEmpty()) {
                issues.add(context.blocker("POM_ASSERTION_CHECKS_PRESENT",
                        "Assertion method requires deterministic checks",
                        assertion.methodName()));
            }
            for (PomCheckSpec check : assertion.checks()) {
                validateCheck(assertion.methodName(), check, locatorIds, context, issues);
            }
        }
    }

    private void validateCheck(
            String methodName,
            PomCheckSpec check,
            Set<String> locatorIds,
            PomContractValidationContext context,
            List<PomContractIssue> issues
    ) {
        if (check.check() == PomCheckType.URL_CONTAINS || check.check() == PomCheckType.URL_EQUALS) {
            if (check.route().isBlank() && check.expectedValue().isBlank()) {
                issues.add(context.blocker("POM_CHECK_ROUTE_OR_EXPECTED_PRESENT",
                        "URL checks require route or expectedValue",
                        methodName + " -> " + check));
            }
            return;
        }
        if (check.locator().isBlank() || !locatorIds.contains(check.locator())) {
            issues.add(context.blocker("POM_CHECK_LOCATOR_EXISTS",
                    "Check must reference an approved locator id",
                    methodName + " -> " + check));
        }
        if ((check.check() == PomCheckType.TEXT_CONTAINS
                || check.check() == PomCheckType.TEXT_EQUALS
                || check.check() == PomCheckType.ATTRIBUTE_EQUALS)
                && check.expectedValue().isBlank()
                && check.valueFrom().isBlank()) {
            issues.add(context.blocker("POM_CHECK_EXPECTED_VALUE_PRESENT",
                    "Text/attribute checks require expectedValue or valueFrom",
                    methodName + " -> " + check));
        }
        if (check.check() == PomCheckType.ATTRIBUTE_EQUALS && check.attribute().isBlank()) {
            issues.add(context.blocker("POM_CHECK_ATTRIBUTE_PRESENT",
                    "Attribute checks require attribute name",
                    methodName + " -> " + check));
        }
    }
}
