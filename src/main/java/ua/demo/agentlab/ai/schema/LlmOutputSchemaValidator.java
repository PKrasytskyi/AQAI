package ua.demo.agentlab.ai.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LlmOutputSchemaValidator {

    private static final Set<String> LOCATOR_STRATEGIES = Set.of("id", "name", "css", "xpath", "partialLinkText");
    private static final Set<String> EXPECTED_STATUSES = Set.of("resolved", "needs-review");
    private static final Set<String> POM_STEP_ACTIONS = Set.of(
            "CLICK",
            "CLEAR_AND_TYPE",
            "SEND_KEYS",
            "SELECT_BY_VISIBLE_TEXT",
            "UPLOAD_FILE",
            "OPEN_ROUTE"
    );
    private static final Set<String> POM_CHECK_TYPES = Set.of(
            "VISIBLE",
            "TEXT_CONTAINS",
            "TEXT_EQUALS",
            "TEXT_PRESENT",
            "URL_CONTAINS",
            "URL_EQUALS",
            "ATTRIBUTE_EQUALS",
            "COUNT_GREATER_THAN",
            "LIST_TEXTS"
    );

    public LlmOutputSchemaValidationReport validatePageObjectSpec(JsonNode root) {
        List<LlmOutputSchemaIssue> issues = new ArrayList<>();
        requireSchemaVersion(root, LlmOutputSchemaVersion.AI_PAGE_OBJECT_SPEC, issues);
        requireArray(root, "pageObjects", "$.pageObjects", issues);
        JsonNode pageObjects = root.path("pageObjects");
        if (pageObjects.isArray() && pageObjects.isEmpty()) {
            issues.add(new LlmOutputSchemaIssue("$.pageObjects", "must contain at least one page object"));
        }
        if (pageObjects.isArray()) {
            for (int index = 0; index < pageObjects.size(); index++) {
                JsonNode item = pageObjects.get(index);
                String path = "$.pageObjects[" + index + "]";
                requireText(item, "pageName", path + ".pageName", issues);
                requireText(item, "openMethodName", path + ".openMethodName", issues);
                requireArray(item, "locators", path + ".locators", issues);
                requireArray(item, "methods", path + ".methods", issues);
                validateLocators(item.path("locators"), path + ".locators", issues);
                validateMethods(item.path("methods"), path + ".methods", issues);
            }
        }
        return report(LlmOutputSchemaVersion.AI_PAGE_OBJECT_SPEC, issues);
    }

    public LlmOutputSchemaValidationReport validateUiTestSpec(JsonNode root) {
        List<LlmOutputSchemaIssue> issues = new ArrayList<>();
        requireSchemaVersion(root, LlmOutputSchemaVersion.AI_UI_TEST_SPEC, issues);
        requireArray(root, "tests", "$.tests", issues);
        JsonNode tests = root.path("tests");
        if (tests.isArray() && tests.isEmpty()) {
            issues.add(new LlmOutputSchemaIssue("$.tests", "must contain at least one test"));
        }
        if (tests.isArray()) {
            for (int index = 0; index < tests.size(); index++) {
                JsonNode item = tests.get(index);
                String path = "$.tests[" + index + "]";
                requireText(item, "scenarioId", path + ".scenarioId", issues);
                requireText(item, "className", path + ".className", issues);
                requireText(item, "pageClassName", path + ".pageClassName", issues);
                requireText(item, "testMethodName", path + ".testMethodName", issues);
                requireText(item, "actionBody", path + ".actionBody", issues);
                requireText(item, "assertionBody", path + ".assertionBody", issues);
                requireArray(item, "additionalImports", path + ".additionalImports", issues);
            }
        }
        return report(LlmOutputSchemaVersion.AI_UI_TEST_SPEC, issues);
    }

    public LlmOutputSchemaValidationReport validatePomContract(JsonNode root) {
        List<LlmOutputSchemaIssue> issues = new ArrayList<>();
        requireSchemaVersion(root, LlmOutputSchemaVersion.POM_CONTRACT, issues);
        JsonNode page = root.path("page");
        if (!page.isObject()) {
            issues.add(new LlmOutputSchemaIssue("$.page", "must be an object"));
        } else {
            requireText(page, "name", "$.page.name", issues);
            requireTextual(page, "route", "$.page.route", issues);
            requireTextual(page, "capability", "$.page.capability", issues);
            requireText(page, "openMethod", "$.page.openMethod", issues);
        }
        requireArray(root, "locators", "$.locators", issues);
        requireArray(root, "actions", "$.actions", issues);
        requireArray(root, "assertions", "$.assertions", issues);
        requireArray(root, "coverageGaps", "$.coverageGaps", issues);
        requireArray(root, "rejectedSuggestions", "$.rejectedSuggestions", issues);
        validatePomLocators(root.path("locators"), "$.locators", issues);
        validatePomActions(root.path("actions"), "$.actions", issues);
        validatePomAssertions(root.path("assertions"), "$.assertions", issues);
        return report(LlmOutputSchemaVersion.POM_CONTRACT, issues);
    }

    public LlmOutputSchemaValidationReport validateResolvedExpectedResult(JsonNode root) {
        List<LlmOutputSchemaIssue> issues = new ArrayList<>();
        requireSchemaVersion(root, LlmOutputSchemaVersion.RESOLVED_EXPECTED_RESULT, issues);
        requireText(root, "testCaseId", "$.testCaseId", issues);
        requireTextual(root, "candidateRequirementId", "$.candidateRequirementId", issues);
        requireTextual(root, "expectedValue", "$.expectedValue", issues);
        requireNumber(root, "confidence", "$.confidence", issues);
        String status = text(root, "status");
        if (!EXPECTED_STATUSES.contains(status)) {
            issues.add(new LlmOutputSchemaIssue("$.status", "must be one of " + EXPECTED_STATUSES));
        }
        requireTextual(root, "rationale", "$.rationale", issues);
        return report(LlmOutputSchemaVersion.RESOLVED_EXPECTED_RESULT, issues);
    }

    public LlmOutputSchemaValidationReport validatePageModelEnrichmentRecord(JsonNode root) {
        List<LlmOutputSchemaIssue> issues = new ArrayList<>();
        requireSchemaVersion(root, LlmOutputSchemaVersion.PAGE_MODEL_ENRICHMENT_RECORD, issues);
        requireText(root, "pageId", "$.pageId", issues);
        requireText(root, "pageName", "$.pageName", issues);
        requireTextual(root, "route", "$.route", issues);
        requireTextual(root, "businessIntent", "$.businessIntent", issues);
        requireTextual(root, "pageSummary", "$.pageSummary", issues);
        for (String field : List.of(
                "supportedActions",
                "stableLocators",
                "preconditions",
                "postconditions",
                "risks",
                "coverageGaps",
                "requirementTraceability"
        )) {
            requireArray(root, field, "$." + field, issues);
        }
        requireNumber(root, "confidenceScore", "$.confidenceScore", issues);
        return report(LlmOutputSchemaVersion.PAGE_MODEL_ENRICHMENT_RECORD, issues);
    }

    public void throwIfInvalid(LlmOutputSchemaValidationReport report) {
        if (report != null && !report.valid()) {
            throw new LlmOutputSchemaValidationException(report);
        }
    }

    private void validateLocators(JsonNode locators, String path, List<LlmOutputSchemaIssue> issues) {
        if (!locators.isArray()) {
            return;
        }
        for (int index = 0; index < locators.size(); index++) {
            JsonNode item = locators.get(index);
            String itemPath = path + "[" + index + "]";
            requireText(item, "fieldName", itemPath + ".fieldName", issues);
            requireTextual(item, "elementName", itemPath + ".elementName", issues);
            String strategy = text(item, "strategy");
            if (!LOCATOR_STRATEGIES.contains(strategy)) {
                issues.add(new LlmOutputSchemaIssue(itemPath + ".strategy", "unsupported locator strategy: " + strategy));
            }
            requireText(item, "value", itemPath + ".value", issues);
        }
    }

    private void validateMethods(JsonNode methods, String path, List<LlmOutputSchemaIssue> issues) {
        if (!methods.isArray()) {
            return;
        }
        for (int index = 0; index < methods.size(); index++) {
            JsonNode item = methods.get(index);
            String itemPath = path + "[" + index + "]";
            requireText(item, "returnType", itemPath + ".returnType", issues);
            requireText(item, "methodName", itemPath + ".methodName", issues);
            requireArray(item, "parameters", itemPath + ".parameters", issues);
            requireText(item, "body", itemPath + ".body", issues);
            requireArray(item, "requiredImports", itemPath + ".requiredImports", issues);
        }
    }

    private void validatePomLocators(JsonNode locators, String path, List<LlmOutputSchemaIssue> issues) {
        if (!locators.isArray()) {
            return;
        }
        for (int index = 0; index < locators.size(); index++) {
            JsonNode item = locators.get(index);
            String itemPath = path + "[" + index + "]";
            requireText(item, "id", itemPath + ".id", issues);
            requireTextual(item, "elementName", itemPath + ".elementName", issues);
            String strategy = text(item, "strategy");
            if (!LOCATOR_STRATEGIES.contains(strategy)) {
                issues.add(new LlmOutputSchemaIssue(itemPath + ".strategy", "unsupported locator strategy: " + strategy));
            }
            requireText(item, "value", itemPath + ".value", issues);
            requireTextual(item, "role", itemPath + ".role", issues);
            requireNumber(item, "stabilityScore", itemPath + ".stabilityScore", issues);
        }
    }

    private void validatePomActions(JsonNode actions, String path, List<LlmOutputSchemaIssue> issues) {
        if (!actions.isArray()) {
            return;
        }
        for (int index = 0; index < actions.size(); index++) {
            JsonNode item = actions.get(index);
            String itemPath = path + "[" + index + "]";
            requireText(item, "methodName", itemPath + ".methodName", issues);
            requireText(item, "kind", itemPath + ".kind", issues);
            String kind = text(item, "kind");
            if (!kind.isBlank() && !"ACTION".equals(kind)) {
                issues.add(new LlmOutputSchemaIssue(itemPath + ".kind", "must be ACTION"));
            }
            requireArray(item, "parameters", itemPath + ".parameters", issues);
            requireArray(item, "steps", itemPath + ".steps", issues);
            JsonNode steps = item.path("steps");
            if (steps.isArray() && steps.isEmpty()) {
                issues.add(new LlmOutputSchemaIssue(itemPath + ".steps", "must contain at least one step"));
            }
            validatePomParameters(item.path("parameters"), itemPath + ".parameters", issues);
            validatePomSteps(steps, itemPath + ".steps", issues);
        }
    }

    private void validatePomParameters(JsonNode parameters, String path, List<LlmOutputSchemaIssue> issues) {
        if (!parameters.isArray()) {
            return;
        }
        for (int index = 0; index < parameters.size(); index++) {
            JsonNode item = parameters.get(index);
            String itemPath = path + "[" + index + "]";
            requireText(item, "type", itemPath + ".type", issues);
            requireText(item, "name", itemPath + ".name", issues);
        }
    }

    private void validatePomSteps(JsonNode steps, String path, List<LlmOutputSchemaIssue> issues) {
        if (!steps.isArray()) {
            return;
        }
        for (int index = 0; index < steps.size(); index++) {
            JsonNode item = steps.get(index);
            String itemPath = path + "[" + index + "]";
            String action = text(item, "action");
            if (!POM_STEP_ACTIONS.contains(action)) {
                issues.add(new LlmOutputSchemaIssue(itemPath + ".action", "unsupported step action: " + action));
            }
            requireTextual(item, "locator", itemPath + ".locator", issues);
            requireTextual(item, "valueFrom", itemPath + ".valueFrom", issues);
            requireTextual(item, "literalValue", itemPath + ".literalValue", issues);
            requireTextual(item, "route", itemPath + ".route", issues);
        }
    }

    private void validatePomAssertions(JsonNode assertions, String path, List<LlmOutputSchemaIssue> issues) {
        if (!assertions.isArray()) {
            return;
        }
        for (int index = 0; index < assertions.size(); index++) {
            JsonNode item = assertions.get(index);
            String itemPath = path + "[" + index + "]";
            requireText(item, "methodName", itemPath + ".methodName", issues);
            String returnType = text(item, "returnType");
            if (!Set.of("boolean", "String", "List<String>").contains(returnType)) {
                issues.add(new LlmOutputSchemaIssue(itemPath + ".returnType", "unsupported return type: " + returnType));
            }
            requireArray(item, "checks", itemPath + ".checks", issues);
            String combine = text(item, "combine");
            if (!Set.of("AND", "OR").contains(combine)) {
                issues.add(new LlmOutputSchemaIssue(itemPath + ".combine", "must be AND or OR"));
            }
            JsonNode checks = item.path("checks");
            if (checks.isArray() && checks.isEmpty()) {
                issues.add(new LlmOutputSchemaIssue(itemPath + ".checks", "must contain at least one check"));
            }
            validatePomChecks(checks, itemPath + ".checks", issues);
        }
    }

    private void validatePomChecks(JsonNode checks, String path, List<LlmOutputSchemaIssue> issues) {
        if (!checks.isArray()) {
            return;
        }
        for (int index = 0; index < checks.size(); index++) {
            JsonNode item = checks.get(index);
            String itemPath = path + "[" + index + "]";
            String check = text(item, "check");
            if (!POM_CHECK_TYPES.contains(check)) {
                issues.add(new LlmOutputSchemaIssue(itemPath + ".check", "unsupported check type: " + check));
            }
            requireTextual(item, "locator", itemPath + ".locator", issues);
            requireTextual(item, "expectedValue", itemPath + ".expectedValue", issues);
            requireTextual(item, "valueFrom", itemPath + ".valueFrom", issues);
            requireTextual(item, "attribute", itemPath + ".attribute", issues);
            requireTextual(item, "route", itemPath + ".route", issues);
        }
    }

    private void requireSchemaVersion(JsonNode node, String expected, List<LlmOutputSchemaIssue> issues) {
        String actual = text(node, "schemaVersion");
        if (!expected.equals(actual)) {
            issues.add(new LlmOutputSchemaIssue("$.schemaVersion", "expected " + expected + " but got " + actual));
        }
    }

    private void requireText(JsonNode node, String field, String path, List<LlmOutputSchemaIssue> issues) {
        if (text(node, field).isBlank()) {
            issues.add(new LlmOutputSchemaIssue(path, "must be a non-empty string"));
        }
    }

    private void requireTextual(JsonNode node, String field, String path, List<LlmOutputSchemaIssue> issues) {
        if (node == null || !node.path(field).isTextual()) {
            issues.add(new LlmOutputSchemaIssue(path, "must be a string"));
        }
    }

    private void requireArray(JsonNode node, String field, String path, List<LlmOutputSchemaIssue> issues) {
        if (node == null || !node.path(field).isArray()) {
            issues.add(new LlmOutputSchemaIssue(path, "must be an array"));
        }
    }

    private void requireNumber(JsonNode node, String field, String path, List<LlmOutputSchemaIssue> issues) {
        if (node == null || !node.path(field).isNumber()) {
            issues.add(new LlmOutputSchemaIssue(path, "must be a number"));
        }
    }

    private String text(JsonNode node, String field) {
        return node == null ? "" : node.path(field).asText("").trim();
    }

    private LlmOutputSchemaValidationReport report(String schemaVersion, List<LlmOutputSchemaIssue> issues) {
        return new LlmOutputSchemaValidationReport(schemaVersion, issues);
    }
}
