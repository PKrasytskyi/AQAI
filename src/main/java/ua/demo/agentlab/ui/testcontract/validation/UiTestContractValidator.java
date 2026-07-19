package ua.demo.agentlab.ui.testcontract.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaValidationReport;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaValidator;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestActionSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestArgumentSource;
import ua.demo.agentlab.ui.testcontract.model.UiTestArgumentSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractBundle;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestDataReferenceSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UiTestContractValidator {

    private static final List<String> FORBIDDEN_INTERNALS = List.of(
            "webdriver", "webelement", "by.", "findelement", "findelements", "driver.",
            "expectedconditions", "thread.sleep", "locator", "cssselector", "xpath"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmOutputSchemaValidator schemaValidator = new LlmOutputSchemaValidator();

    public UiTestContractValidationResult validate(UiTestContractValidationInput input) {
        UiTestContractBundle bundle = input == null ? null : input.bundle();
        LlmOutputSchemaValidationReport schemaReport = bundle == null
                ? new LlmOutputSchemaValidationReport(
                LlmOutputSchemaVersion.UI_TEST_CONTRACT_BUNDLE,
                List.of(new ua.demo.agentlab.ai.schema.LlmOutputSchemaIssue("$", "bundle is required"))
        )
                : schemaValidator.validateUiTestContractBundle(objectMapper.valueToTree(bundle));
        UiTestContractQualityReport qualityReport = validateQuality(input);
        return new UiTestContractValidationResult(schemaReport, qualityReport);
    }

    private UiTestContractQualityReport validateQuality(UiTestContractValidationInput input) {
        List<UiTestContractIssue> issues = new ArrayList<>();
        if (input == null || input.bundle() == null) {
            issues.add(blocker("TEST_CONTRACT_BUNDLE_REQUIRED", "", "Typed test contract bundle is required", ""));
            return report(0, issues);
        }
        Map<String, PomContractSpec> pages = new LinkedHashMap<>();
        for (PomContractSpec contract : input.pomContracts()) {
            pages.put(contract.page().name(), contract);
        }
        validateScenarioCoverage(input, issues);
        Set<String> classNames = new HashSet<>();
        Set<String> methodNames = new HashSet<>();
        for (UiTestContractSpec contract : input.bundle().contracts()) {
            if (!classNames.add(contract.className())) {
                issues.add(blocker(
                        "TEST_CONTRACT_CLASS_UNIQUE",
                        contract.scenarioId(),
                        "Generated test class name must be unique",
                        contract.className()
                ));
            }
            if (!methodNames.add(contract.testMethodName())) {
                issues.add(blocker(
                        "TEST_CONTRACT_METHOD_UNIQUE",
                        contract.scenarioId(),
                        "Generated test method name must be unique",
                        contract.testMethodName()
                ));
            }
            validatePages(contract, pages, issues);
            validateActions(contract, contract.preconditions(), pages, issues);
            validateActions(contract, contract.actions(), pages, issues);
            validateAssertions(contract, pages, issues);
            validateDataReferences(contract, issues);
            validateAtomicSetup(contract, pages, issues);
            validateForbiddenInternals(contract, issues);
            for (String gap : contract.coverageGaps()) {
                issues.add(new UiTestContractIssue(
                        UiTestContractIssueSeverity.WARNING,
                        "TEST_CONTRACT_COVERAGE_GAP",
                        contract.scenarioId(),
                        "Unsupported assertion or action remains review evidence and is not emitted as Java",
                        gap
                ));
            }
        }
        return report(input.bundle().contracts().size(), issues);
    }

    private void validateScenarioCoverage(UiTestContractValidationInput input, List<UiTestContractIssue> issues) {
        if (input.canonicalTestCases() == null) {
            issues.add(blocker(
                    "TEST_CONTRACT_CANONICAL_SOURCE_REQUIRED",
                    "",
                    "Canonical test-case bundle is required for one-to-one traceability",
                    ""
            ));
            return;
        }
        Set<String> expected = new LinkedHashSet<>();
        input.canonicalTestCases().testCases().forEach(testCase -> expected.add(testCase.id()));
        Set<String> actual = new LinkedHashSet<>();
        input.bundle().contracts().forEach(contract -> actual.add(contract.scenarioId()));
        if (!expected.equals(actual)) {
            issues.add(blocker(
                    "TEST_CONTRACT_ONE_PER_SCENARIO",
                    "",
                    "Exactly one typed test contract is required per canonical executable scenario",
                    "expected=" + expected + ", actual=" + actual
            ));
        }
    }

    private void validatePages(
            UiTestContractSpec contract,
            Map<String, PomContractSpec> pages,
            List<UiTestContractIssue> issues
    ) {
        Set<String> referencedPages = new LinkedHashSet<>();
        referencedPages.add(contract.sourcePage());
        referencedPages.add(contract.targetPage());
        for (String page : referencedPages) {
            if (!pages.containsKey(page)) {
                issues.add(blocker(
                        "TEST_CONTRACT_PAGE_DECLARED",
                        contract.scenarioId(),
                        "Referenced page must exist in validated POM contracts",
                        page
                ));
            }
        }
    }

    private void validateActions(
            UiTestContractSpec contract,
            List<UiTestActionSpec> actions,
            Map<String, PomContractSpec> pages,
            List<UiTestContractIssue> issues
    ) {
        int expectedOrder = 1;
        for (UiTestActionSpec action : actions) {
            if (action.order() != expectedOrder++) {
                issues.add(blocker(
                        "TEST_CONTRACT_ACTION_ORDER",
                        contract.scenarioId(),
                        "Action order must be contiguous and deterministic",
                        action.page() + "#" + action.method() + " order=" + action.order()
                ));
            }
            PomContractSpec page = pages.get(action.page());
            if (page == null) {
                continue;
            }
            if (action.method().equals(page.page().openMethod())) {
                if (!action.arguments().isEmpty()) {
                    issues.add(blocker(
                            "TEST_CONTRACT_OPEN_ARGUMENTS",
                            contract.scenarioId(),
                            "POM open method must not receive test arguments",
                            action.page() + "#" + action.method()
                    ));
                }
                continue;
            }
            PomActionSpec method = page.actions().stream()
                    .filter(candidate -> candidate.methodName().equals(action.method()))
                    .findFirst()
                    .orElse(null);
            if (method == null) {
                issues.add(blocker(
                        "TEST_CONTRACT_ACTION_DECLARED",
                        contract.scenarioId(),
                        "Action must reference a public method owned by the selected POM",
                        action.page() + "#" + action.method()
                ));
                continue;
            }
            if (method.parameters().size() != action.arguments().size()) {
                issues.add(blocker(
                        "TEST_CONTRACT_ACTION_ARGUMENT_COUNT",
                        contract.scenarioId(),
                        "Action argument count must match the POM method signature",
                        action.page() + "#" + action.method()
                ));
            }
        }
    }

    private void validateAssertions(
            UiTestContractSpec contract,
            Map<String, PomContractSpec> pages,
            List<UiTestContractIssue> issues
    ) {
        if (contract.assertions().isEmpty()) {
            issues.add(blocker(
                    "TEST_CONTRACT_ASSERTION_REQUIRED",
                    contract.scenarioId(),
                    "Atomic test contract must contain at least one typed assertion",
                    ""
            ));
            return;
        }
        int expectedOrder = 1;
        for (var assertion : contract.assertions()) {
            if (assertion.order() != expectedOrder++) {
                issues.add(blocker(
                        "TEST_CONTRACT_ASSERTION_ORDER",
                        contract.scenarioId(),
                        "Assertion order must be contiguous and deterministic",
                        assertion.page() + "#" + assertion.method()
                ));
            }
            PomContractSpec page = pages.get(assertion.page());
            PomAssertionSpec method = page == null ? null : page.assertions().stream()
                    .filter(candidate -> candidate.methodName().equals(assertion.method()))
                    .findFirst()
                    .orElse(null);
            if (method == null) {
                issues.add(blocker(
                        "TEST_CONTRACT_ASSERTION_DECLARED",
                        contract.scenarioId(),
                        "Assertion must reference a public assertion method owned by the selected POM",
                        assertion.page() + "#" + assertion.method()
                ));
            }
            if (assertion.expectedValue().isBlank()) {
                issues.add(blocker(
                        "TEST_CONTRACT_EXPECTED_VALUE_RESOLVED",
                        contract.scenarioId(),
                        "Assertion expected value must be resolved",
                        assertion.requirementId()
                ));
            }
        }
    }

    private void validateDataReferences(UiTestContractSpec contract, List<UiTestContractIssue> issues) {
        Map<String, UiTestDataReferenceSpec> references = new LinkedHashMap<>();
        for (UiTestDataReferenceSpec reference : contract.dataReferences()) {
            if (references.put(reference.id(), reference) != null) {
                issues.add(blocker(
                        "TEST_CONTRACT_DATA_REFERENCE_UNIQUE",
                        contract.scenarioId(),
                        "Data reference IDs must be unique inside one test",
                        reference.id()
                ));
            }
        }
        Set<String> used = new LinkedHashSet<>();
        java.util.stream.Stream.concat(contract.preconditions().stream(), contract.actions().stream())
                .flatMap(action -> action.arguments().stream())
                .forEach(argument -> validateArgument(contract, argument, references, used, issues));
        for (String reference : references.keySet()) {
            if (!used.contains(reference)) {
                issues.add(blocker(
                        "TEST_CONTRACT_DATA_REFERENCE_USED",
                        contract.scenarioId(),
                        "Declared scenario data must be used by a typed method argument",
                        reference
                ));
            }
        }
    }

    private void validateArgument(
            UiTestContractSpec contract,
            UiTestArgumentSpec argument,
            Map<String, UiTestDataReferenceSpec> references,
            Set<String> used,
            List<UiTestContractIssue> issues
    ) {
        if (argument.source() == UiTestArgumentSource.DATA_REFERENCE) {
            if (!references.containsKey(argument.referenceId())) {
                issues.add(blocker(
                        "TEST_CONTRACT_DATA_REFERENCE_DECLARED",
                        contract.scenarioId(),
                        "Method argument must reference declared typed scenario data",
                        argument.referenceId()
                ));
            } else {
                used.add(argument.referenceId());
            }
            if (argument.field().isBlank()) {
                issues.add(blocker(
                        "TEST_CONTRACT_DATA_FIELD_REQUIRED",
                        contract.scenarioId(),
                        "Data-reference argument must select a typed field",
                        argument.parameterName()
                ));
            }
        }
        String parameter = argument.parameterName().toLowerCase(Locale.ROOT);
        if (argument.source() == UiTestArgumentSource.LITERAL
                && (parameter.contains("password") || parameter.contains("username") || parameter.contains("token"))) {
            issues.add(blocker(
                    "TEST_CONTRACT_NO_LITERAL_SECRETS",
                    contract.scenarioId(),
                    "Credentials and tokens must use typed data references, never literals",
                    argument.parameterName()
            ));
        }
    }

    private void validateAtomicSetup(
            UiTestContractSpec contract,
            Map<String, PomContractSpec> pages,
            List<UiTestContractIssue> issues
    ) {
        boolean authenticates = java.util.stream.Stream.concat(
                        contract.preconditions().stream(),
                        contract.actions().stream()
                )
                .flatMap(action -> action.sourceOperations().stream())
                .anyMatch("AUTHENTICATE"::equals);
        if (!authenticates) {
            return;
        }
        boolean opensAuthenticationSource = contract.preconditions().stream().anyMatch(action -> {
            PomContractSpec page = pages.get(action.page());
            return page != null && action.method().equals(page.page().openMethod());
        });
        if (!opensAuthenticationSource) {
            issues.add(blocker(
                    "TEST_CONTRACT_SELF_CONTAINED_SETUP",
                    contract.scenarioId(),
                    "Authentication scenario must open its source page inside the same atomic test",
                    contract.sourcePage()
            ));
        }
    }

    private void validateForbiddenInternals(UiTestContractSpec contract, List<UiTestContractIssue> issues) {
        List<String> structuralValues = new ArrayList<>(List.of(
                contract.className(), contract.testMethodName(), contract.sourcePage(), contract.targetPage()
        ));
        java.util.stream.Stream.concat(contract.preconditions().stream(), contract.actions().stream())
                .forEach(action -> {
                    structuralValues.add(action.page());
                    structuralValues.add(action.method());
                    action.arguments().forEach(argument -> structuralValues.add(argument.literalValue()));
                });
        for (String value : structuralValues) {
            String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
            for (String forbidden : FORBIDDEN_INTERNALS) {
                if (normalized.contains(forbidden)) {
                    issues.add(blocker(
                            "TEST_CONTRACT_NO_UI_INTERNALS",
                            contract.scenarioId(),
                            "Typed test contract must not contain Selenium, driver, wait, or locator internals",
                            value
                    ));
                }
            }
        }
    }

    private UiTestContractQualityReport report(int contractCount, List<UiTestContractIssue> issues) {
        Set<String> blockedScenarios = new HashSet<>();
        for (UiTestContractIssue issue : issues) {
            if (issue.severity() == UiTestContractIssueSeverity.BLOCKER && !issue.scenarioId().isBlank()) {
                blockedScenarios.add(issue.scenarioId());
            }
        }
        boolean globalBlocker = issues.stream().anyMatch(issue -> issue.severity() == UiTestContractIssueSeverity.BLOCKER
                && issue.scenarioId().isBlank());
        int needsReview = globalBlocker ? contractCount : blockedScenarios.size();
        return new UiTestContractQualityReport(
                LlmOutputSchemaVersion.UI_TEST_CONTRACT_BUNDLE,
                contractCount,
                Math.max(0, contractCount - needsReview),
                needsReview,
                issues
        );
    }

    private UiTestContractIssue blocker(String ruleId, String scenarioId, String message, String evidence) {
        return new UiTestContractIssue(
                UiTestContractIssueSeverity.BLOCKER,
                ruleId,
                scenarioId,
                message,
                evidence
        );
    }
}
