package ua.demo.agentlab.ai.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LlmOutputSchemaValidator {

    private static final Set<String> LOCATOR_STRATEGIES = Set.of("id", "name", "css", "xpath", "partialLinkText");
    private static final Set<String> EXPECTED_STATUSES = Set.of("resolved", "needs-review");

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
