package ua.demo.agentlab.ai.ui.contract;

import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PomContractQualityGate {

    private static final Set<String> LOCATOR_STRATEGIES = Set.of("id", "name", "css", "xpath", "partialLinkText");
    private static final Set<String> ALLOWED_RETURN_TYPES = Set.of("boolean", "String", "List<String>");

    public PomContractQualityReport validate(PomContractSpec spec) {
        List<PomContractIssue> issues = new ArrayList<>();
        if (spec == null) {
            issues.add(blocker("POM_CONTRACT_PRESENT", "POM contract must be present", "null"));
            return new PomContractQualityReport(LlmOutputSchemaVersion.POM_CONTRACT, "", issues);
        }
        if (!LlmOutputSchemaVersion.POM_CONTRACT.equals(spec.schemaVersion())) {
            issues.add(blocker("POM_CONTRACT_SCHEMA_VERSION",
                    "POM contract schema version must match",
                    spec.schemaVersion()));
        }
        validatePage(spec.page(), issues);
        Set<String> locatorIds = validateLocators(spec.locators(), issues);
        validateComponents(spec.components(), locatorIds, issues);
        validateActions(spec.actions(), locatorIds, issues);
        validateAssertions(spec.assertions(), locatorIds, issues);
        boolean hasComponentBehavior = spec.components().stream()
                .anyMatch(component -> !component.actions().isEmpty() || !component.assertions().isEmpty());
        if (spec.actions().isEmpty() && spec.assertions().isEmpty()
                && !hasComponentBehavior && spec.coverageGaps().isEmpty()) {
            issues.add(blocker("POM_CONTRACT_HAS_BEHAVIOR_OR_GAPS",
                    "POM contract must contain actions, assertions, or explicit coverage gaps",
                    spec.page().name()));
        }
        return new PomContractQualityReport(spec.schemaVersion(), spec.page().name(), issues);
    }

    private void validatePage(PomPageSpec page, List<PomContractIssue> issues) {
        if (page == null || page.name().isBlank()) {
            issues.add(blocker("POM_PAGE_NAME_PRESENT", "Page name is required", String.valueOf(page)));
        }
        if (page == null || page.openMethod().isBlank()) {
            issues.add(blocker("POM_OPEN_METHOD_PRESENT", "Open method is required", String.valueOf(page)));
        }
    }

    private Set<String> validateLocators(List<PomLocatorSpec> locators, List<PomContractIssue> issues) {
        Set<String> ids = new LinkedHashSet<>();
        for (PomLocatorSpec locator : locators) {
            if (locator.id().isBlank()) {
                issues.add(blocker("POM_LOCATOR_ID_PRESENT", "Locator id is required", locator.toString()));
                continue;
            }
            if (!ids.add(locator.id())) {
                issues.add(blocker("POM_LOCATOR_ID_UNIQUE", "Locator ids must be unique", locator.id()));
            }
            if (!LOCATOR_STRATEGIES.contains(locator.strategy())) {
                issues.add(blocker("POM_LOCATOR_STRATEGY_SUPPORTED",
                        "Locator strategy is not supported",
                        locator.id() + " -> " + locator.strategy()));
            }
            if (locator.value().isBlank()) {
                issues.add(blocker("POM_LOCATOR_VALUE_PRESENT", "Locator value is required", locator.id()));
            }
            if (locator.stabilityScore() > 0.0d && locator.stabilityScore() < 0.50d) {
                issues.add(warning("POM_LOCATOR_LOW_STABILITY",
                        "Locator stability is low",
                        locator.id() + " -> " + locator.stabilityScore()));
            }
        }
        return ids;
    }

    private void validateComponents(
            List<PomComponentSpec> components,
            Set<String> pageLocatorIds,
            List<PomContractIssue> issues
    ) {
        Set<String> componentNames = new LinkedHashSet<>();
        for (PomComponentSpec component : components) {
            if (component.name().isBlank()) {
                issues.add(blocker("POM_COMPONENT_NAME_PRESENT", "Component name is required", component.toString()));
                continue;
            }
            if (!componentNames.add(component.name())) {
                issues.add(blocker("POM_COMPONENT_NAME_UNIQUE", "Component names must be unique", component.name()));
            }
            Set<String> componentLocatorIds = validateLocators(component.locators(), issues);
            Set<String> allowedIds = new LinkedHashSet<>(componentLocatorIds);
            allowedIds.addAll(pageLocatorIds);
            if (!component.rootLocatorId().isBlank()
                    && !pageLocatorIds.contains(component.rootLocatorId())
                    && !componentLocatorIds.contains(component.rootLocatorId())) {
                issues.add(blocker("POM_COMPONENT_ROOT_LOCATOR_EXISTS",
                        "Component rootLocatorId must reference a page or component locator",
                        component.name() + " -> " + component.rootLocatorId()));
            }
            validateActions(component.actions(), allowedIds, issues);
            validateAssertions(component.assertions(), allowedIds, issues);
        }
    }

    private void validateActions(
            List<PomActionSpec> actions,
            Set<String> locatorIds,
            List<PomContractIssue> issues
    ) {
        Set<String> methodNames = new LinkedHashSet<>();
        for (PomActionSpec action : actions) {
            if (action.methodName().isBlank()) {
                issues.add(blocker("POM_ACTION_METHOD_PRESENT", "Action method name is required", action.toString()));
            }
            if (!methodNames.add(action.methodName())) {
                issues.add(blocker("POM_ACTION_METHOD_UNIQUE", "Action method names must be unique", action.methodName()));
            }
            if (action.steps().isEmpty()) {
                issues.add(blocker("POM_ACTION_STEPS_PRESENT", "Action method requires deterministic steps", action.methodName()));
            }
            validateParameters(action.methodName(), action.parameters(), issues);
            for (PomStepSpec step : action.steps()) {
                validateStep(action.methodName(), step, locatorIds, issues);
            }
        }
    }

    private void validateParameters(
            String methodName,
            List<AiMethodParameterSpec> parameters,
            List<PomContractIssue> issues
    ) {
        Set<String> names = new LinkedHashSet<>();
        for (AiMethodParameterSpec parameter : parameters) {
            if (parameter.type().isBlank() || parameter.name().isBlank()) {
                issues.add(blocker("POM_ACTION_PARAMETER_COMPLETE",
                        "Action parameters require type and name",
                        methodName + " -> " + parameter));
            }
            if (!names.add(parameter.name())) {
                issues.add(blocker("POM_ACTION_PARAMETER_UNIQUE",
                        "Action parameter names must be unique",
                        methodName + " -> " + parameter.name()));
            }
        }
    }

    private void validateStep(
            String methodName,
            PomStepSpec step,
            Set<String> locatorIds,
            List<PomContractIssue> issues
    ) {
        if (step.action() == PomStepAction.OPEN_ROUTE) {
            if (step.route().isBlank()) {
                issues.add(blocker("POM_STEP_ROUTE_PRESENT", "OPEN_ROUTE requires route", methodName));
            }
            return;
        }
        if (step.locator().isBlank() || !locatorIds.contains(step.locator())) {
            issues.add(blocker("POM_STEP_LOCATOR_EXISTS",
                    "Step must reference an approved locator id",
                    methodName + " -> " + step));
        }
        if (Set.of(PomStepAction.CLEAR_AND_TYPE, PomStepAction.SEND_KEYS, PomStepAction.SELECT_BY_VISIBLE_TEXT,
                PomStepAction.UPLOAD_FILE).contains(step.action())
                && step.valueFrom().isBlank()
                && step.literalValue().isBlank()) {
            issues.add(blocker("POM_STEP_VALUE_PRESENT",
                    "Input/select/upload steps require valueFrom or literalValue",
                    methodName + " -> " + step));
        }
    }

    private void validateAssertions(
            List<PomAssertionSpec> assertions,
            Set<String> locatorIds,
            List<PomContractIssue> issues
    ) {
        Set<String> methodNames = new LinkedHashSet<>();
        for (PomAssertionSpec assertion : assertions) {
            if (assertion.methodName().isBlank()) {
                issues.add(blocker("POM_ASSERTION_METHOD_PRESENT", "Assertion method name is required", assertion.toString()));
            }
            if (!methodNames.add(assertion.methodName())) {
                issues.add(blocker("POM_ASSERTION_METHOD_UNIQUE",
                        "Assertion method names must be unique",
                        assertion.methodName()));
            }
            if (!ALLOWED_RETURN_TYPES.contains(assertion.returnType())) {
                issues.add(blocker("POM_ASSERTION_RETURN_TYPE_SUPPORTED",
                        "Assertion return type is not supported",
                        assertion.methodName() + " -> " + assertion.returnType()));
            }
            if (assertion.checks().isEmpty()) {
                issues.add(blocker("POM_ASSERTION_CHECKS_PRESENT",
                        "Assertion method requires deterministic checks",
                        assertion.methodName()));
            }
            for (PomCheckSpec check : assertion.checks()) {
                validateCheck(assertion.methodName(), check, locatorIds, issues);
            }
        }
    }

    private void validateCheck(
            String methodName,
            PomCheckSpec check,
            Set<String> locatorIds,
            List<PomContractIssue> issues
    ) {
        if (check.check() == PomCheckType.URL_CONTAINS || check.check() == PomCheckType.URL_EQUALS) {
            if (check.route().isBlank() && check.expectedValue().isBlank()) {
                issues.add(blocker("POM_CHECK_ROUTE_OR_EXPECTED_PRESENT",
                        "URL checks require route or expectedValue",
                        methodName + " -> " + check));
            }
            return;
        }
        if (check.locator().isBlank() || !locatorIds.contains(check.locator())) {
            issues.add(blocker("POM_CHECK_LOCATOR_EXISTS",
                    "Check must reference an approved locator id",
                    methodName + " -> " + check));
        }
        if ((check.check() == PomCheckType.TEXT_CONTAINS
                || check.check() == PomCheckType.TEXT_EQUALS
                || check.check() == PomCheckType.ATTRIBUTE_EQUALS)
                && check.expectedValue().isBlank()
                && check.valueFrom().isBlank()) {
            issues.add(blocker("POM_CHECK_EXPECTED_VALUE_PRESENT",
                    "Text/attribute checks require expectedValue or valueFrom",
                    methodName + " -> " + check));
        }
        if (check.check() == PomCheckType.ATTRIBUTE_EQUALS && check.attribute().isBlank()) {
            issues.add(blocker("POM_CHECK_ATTRIBUTE_PRESENT",
                    "Attribute checks require attribute name",
                    methodName + " -> " + check));
        }
    }

    private PomContractIssue blocker(String ruleId, String message, String evidence) {
        return new PomContractIssue(PomContractSeverity.BLOCKER, ruleId, message, evidence);
    }

    private PomContractIssue warning(String ruleId, String message, String evidence) {
        return new PomContractIssue(PomContractSeverity.WARNING, ruleId, message, evidence);
    }
}
