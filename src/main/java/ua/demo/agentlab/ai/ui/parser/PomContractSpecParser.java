package ua.demo.agentlab.ai.ui.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaValidator;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;

import java.util.LinkedHashSet;
import java.util.Set;

public class PomContractSpecParser extends StructuredOutputParserSupport {

    private final LlmOutputSchemaValidator schemaValidator = new LlmOutputSchemaValidator();

    public PomContractSpec parse(String rawResponse) {
        JsonNode root = sanitize(parseRoot(rawResponse));
        schemaValidator.throwIfInvalid(schemaValidator.validatePomContract(root));
        try {
            return objectMapper.treeToValue(root, PomContractSpec.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse POM contract spec: " + rootCauseMessage(exception), exception);
        }
    }

    private JsonNode sanitize(JsonNode root) {
        if (!(root instanceof ObjectNode object)) {
            return root;
        }
        sanitizeLocators(object);
        sanitizeComponents(object);
        sanitizeActions(object);
        sanitizeAssertions(object);
        sanitizeCoverageGaps(object);
        return object;
    }

    private void sanitizeLocators(ObjectNode root) {
        JsonNode locatorsNode = root.path("locators");
        if (!locatorsNode.isArray()) {
            return;
        }
        ArrayNode sanitized = objectMapper.createArrayNode();
        for (JsonNode locatorNode : locatorsNode) {
            if (!(locatorNode instanceof ObjectNode locator)) {
                continue;
            }
            if (locator.path("elements").isArray()) {
                for (JsonNode nested : locator.path("elements")) {
                    if (nested instanceof ObjectNode nestedLocator) {
                        sanitizeLocator(nestedLocator);
                        sanitized.add(nestedLocator);
                    }
                }
                continue;
            }
            sanitizeLocator(locator);
            sanitized.add(locator);
        }
        root.set("locators", sanitized);
    }

    private void sanitizeLocator(ObjectNode locator) {
        String id = text(locator, "id");
        if (text(locator, "elementName").isBlank()) {
            locator.put("elementName", firstNonBlank(text(locator, "element"), id, text(locator, "value"), "element"));
        }
        if (text(locator, "role").isBlank()) {
            locator.put("role", inferRole(id, text(locator, "value")));
        }
        if (!locator.path("stabilityScore").isNumber()) {
            locator.put("stabilityScore", locator.path("score").isNumber()
                    ? locator.path("score").asDouble()
                    : 0.75d);
        }
        locator.remove("element");
        locator.remove("component");
        locator.remove("componentName");
        locator.remove("type");
        locator.remove("root");
        locator.remove("scope");
        locator.remove("elements");
        removeMapperLocatorMetadata(locator);
    }

    private void removeMapperLocatorMetadata(ObjectNode locator) {
        locator.remove("score");
        locator.remove("evidenceSource");
        locator.remove("evidenceType");
        locator.remove("accessibleName");
        locator.remove("visibleText");
        locator.remove("href");
        locator.remove("originHost");
        locator.remove("sameOrigin");
        locator.remove("uniqueOnPage");
        locator.remove("uniqueWithinComponent");
        locator.remove("stableAcrossRuns");
        locator.remove("globalMatchCount");
        locator.remove("componentMatchCount");
        locator.remove("globalCount");
        locator.remove("scopedCount");
        locator.remove("risks");
    }

    private void sanitizeComponents(ObjectNode root) {
        JsonNode componentsNode = root.path("components");
        if (!componentsNode.isArray()) {
            root.set("components", objectMapper.createArrayNode());
            return;
        }
        ArrayNode sanitized = objectMapper.createArrayNode();
        for (JsonNode componentNode : componentsNode) {
            if (!(componentNode instanceof ObjectNode component)) {
                continue;
            }
            boolean schemaShaped = !text(component, "name").isBlank()
                    && component.path("locators").isArray()
                    && component.path("actions").isArray()
                    && component.path("assertions").isArray()
                    && component.path("reusable").isBoolean();
            boolean hasBehavior = component.path("actions").isArray() && !component.path("actions").isEmpty()
                    || component.path("assertions").isArray() && !component.path("assertions").isEmpty();
            if (schemaShaped && hasBehavior) {
                sanitized.add(component);
            }
        }
        root.set("components", sanitized);
    }

    private void sanitizeActions(ObjectNode root) {
        JsonNode actionsNode = root.path("actions");
        if (!actionsNode.isArray()) {
            return;
        }
        ArrayNode sanitizedActions = objectMapper.createArrayNode();
        ArrayNode coverageGaps = coverageGaps(root);
        for (JsonNode actionNode : actionsNode) {
            if (!(actionNode instanceof ObjectNode action)) {
                continue;
            }
            removeEvidenceMetadata(action);
            ArrayNode steps = objectMapper.createArrayNode();
            Set<String> parameterNames = parameterNames(action.path("parameters"));
            for (JsonNode stepNode : action.path("steps")) {
                if (!(stepNode instanceof ObjectNode step)) {
                    continue;
                }
                String rawAction = text(step, "action");
                String normalizedAction = normalizeStepAction(rawAction);
                if (normalizedAction.isBlank()) {
                    coverageGaps.add(actionGap(action, rawAction));
                    continue;
                }
                step.put("action", normalizedAction);
                normalizeStepValueFrom(step, normalizedAction, parameterNames);
                removeEvidenceMetadata(step);
                steps.add(step);
            }
            action.set("steps", steps);
            if (steps.isEmpty()) {
                coverageGaps.add(actionGap(action, "no schema-compatible steps"));
                continue;
            }
            sanitizedActions.add(action);
        }
        root.set("actions", sanitizedActions);
    }

    private void sanitizeAssertions(ObjectNode root) {
        JsonNode assertionsNode = root.path("assertions");
        if (!assertionsNode.isArray()) {
            return;
        }
        for (JsonNode assertionNode : assertionsNode) {
            if (!(assertionNode instanceof ObjectNode assertion)) {
                continue;
            }
            removeEvidenceMetadata(assertion);
            for (JsonNode checkNode : assertion.path("checks")) {
                if (checkNode instanceof ObjectNode check) {
                    String rawCheck = text(check, "check");
                    String normalized = normalizeCheck(rawCheck);
                    if (!normalized.isBlank()) {
                        check.put("check", normalized);
                    }
                    removeEvidenceMetadata(check);
                }
            }
        }
    }

    private void removeEvidenceMetadata(ObjectNode node) {
        node.remove("confidence");
        node.remove("confidenceScore");
        node.remove("score");
        node.remove("source");
        node.remove("sourceReference");
        node.remove("requirementId");
        node.remove("requirementIds");
        node.remove("testCaseId");
        node.remove("ownerPage");
        node.remove("pageName");
        node.remove("pageRoute");
        node.remove("evidence");
        node.remove("evidenceSource");
        node.remove("rationale");
        node.remove("description");
        node.remove("note");
        node.remove("notes");
        node.remove("comment");
        node.remove("comments");
        node.remove("risks");
    }

    private void sanitizeCoverageGaps(ObjectNode root) {
        JsonNode coverageNode = root.path("coverageGaps");
        if (!coverageNode.isArray()) {
            root.set("coverageGaps", objectMapper.createArrayNode());
            return;
        }
        ArrayNode sanitized = objectMapper.createArrayNode();
        for (JsonNode item : coverageNode) {
            String gap = "";
            if (item.isTextual()) {
                gap = item.asText();
            } else if (item.isObject()) {
                gap = coverageGapText(item);
            }
            if (keepCoverageGap(root, gap)) {
                sanitized.add(gap);
            }
        }
        root.set("coverageGaps", sanitized);
    }

    private ArrayNode coverageGaps(ObjectNode root) {
        JsonNode node = root.path("coverageGaps");
        if (node instanceof ArrayNode array) {
            return array;
        }
        ArrayNode array = objectMapper.createArrayNode();
        root.set("coverageGaps", array);
        return array;
    }

    private Set<String> parameterNames(JsonNode parameters) {
        Set<String> names = new LinkedHashSet<>();
        if (parameters != null && parameters.isArray()) {
            for (JsonNode parameter : parameters) {
                String name = text(parameter, "name");
                if (!name.isBlank()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private void normalizeStepValueFrom(ObjectNode step, String normalizedAction, Set<String> parameterNames) {
        String valueFrom = text(step, "valueFrom");
        if (parameterNames.isEmpty()) {
            return;
        }
        if (valueFrom.isBlank() && inputStep(normalizedAction)) {
            String inferred = inferParameterName(step, parameterNames);
            if (!inferred.isBlank()) {
                step.put("valueFrom", inferred);
            }
            return;
        }
        if ("parameter".equalsIgnoreCase(valueFrom)) {
            String inferred = inferParameterName(step, parameterNames);
            if (!inferred.isBlank()) {
                step.put("valueFrom", inferred);
            }
        }
    }

    private boolean inputStep(String normalizedAction) {
        return "CLEAR_AND_TYPE".equals(normalizedAction)
                || "SEND_KEYS".equals(normalizedAction)
                || "SELECT_BY_VISIBLE_TEXT".equals(normalizedAction)
                || "UPLOAD_FILE".equals(normalizedAction);
    }

    private String inferParameterName(ObjectNode step, Set<String> parameterNames) {
        String locator = text(step, "locator").toLowerCase(java.util.Locale.ROOT);
        if (locator.contains("password") && parameterNames.contains("password")) {
            return "password";
        }
        if ((locator.contains("user") || locator.contains("email")) && parameterNames.contains("username")) {
            return "username";
        }
        if (parameterNames.size() == 1) {
            return parameterNames.iterator().next();
        }
        String methodHint = text(step, "methodName").toLowerCase(java.util.Locale.ROOT);
        for (String parameterName : parameterNames) {
            if (!parameterName.isBlank() && methodHint.contains(parameterName.toLowerCase(java.util.Locale.ROOT))) {
                return parameterName;
            }
        }
        return "";
    }

    private String normalizeStepAction(String rawAction) {
        return switch (rawAction == null ? "" : rawAction.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "CLICK" -> "CLICK";
            case "TYPE", "CLEAR_TYPE", "CLEARANDTYPE", "CLEAR_AND_TYPE" -> "CLEAR_AND_TYPE";
            case "SEND_KEYS", "SENDKEYS" -> "SEND_KEYS";
            case "SELECT", "SELECT_BY_VISIBLE_TEXT" -> "SELECT_BY_VISIBLE_TEXT";
            case "UPLOAD", "UPLOAD_FILE" -> "UPLOAD_FILE";
            case "OPEN", "OPEN_ROUTE" -> "OPEN_ROUTE";
            case "OPEN_ROUTE_CHECK", "COVERAGE_GAP" -> "";
            default -> "";
        };
    }

    private String normalizeCheck(String rawCheck) {
        return switch (rawCheck == null ? "" : rawCheck.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "VISIBLE", "ELEMENT_VISIBLE", "FORM_VISIBLE" -> "VISIBLE";
            case "TEXT_CONTAINS" -> "TEXT_CONTAINS";
            case "TEXT_EQUALS" -> "TEXT_EQUALS";
            case "TEXT_PRESENT", "CONTENT_VISIBLE" -> "TEXT_PRESENT";
            case "URL_CONTAINS", "ROUTE_CONTAINS" -> "URL_CONTAINS";
            case "URL_EQUALS", "ROUTE_EQUALS" -> "URL_EQUALS";
            case "ATTRIBUTE_EQUALS" -> "ATTRIBUTE_EQUALS";
            case "COUNT_GREATER_THAN" -> "COUNT_GREATER_THAN";
            case "LIST_TEXTS" -> "LIST_TEXTS";
            default -> rawCheck == null ? "" : rawCheck.trim().toUpperCase(java.util.Locale.ROOT);
        };
    }

    private String actionGap(ObjectNode action, String reason) {
        String methodName = text(action, "methodName");
        return "Dropped unsupported POM action step"
                + (methodName.isBlank() ? "" : " in " + methodName)
                + (reason == null || reason.isBlank() ? "" : ": " + reason);
    }

    private String coverageGapText(JsonNode item) {
        String id = text(item, "id");
        String area = text(item, "area");
        String detail = text(item, "detail");
        StringBuilder builder = new StringBuilder();
        if (!id.isBlank()) {
            builder.append(id).append(": ");
        }
        if (!area.isBlank()) {
            builder.append(area);
        }
        if (!detail.isBlank()) {
            if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != ' ') {
                builder.append(" - ");
            }
            builder.append(detail);
        }
        return builder.isEmpty() ? item.toString() : builder.toString();
    }

    private boolean keepCoverageGap(ObjectNode root, String gap) {
        if (gap == null || gap.isBlank()) {
            return false;
        }
        String pageName = text(root.path("page"), "name").toLowerCase(java.util.Locale.ROOT);
        String normalizedGap = gap.toLowerCase(java.util.Locale.ROOT);
        if (pageName.contains("login")) {
            if (normalizedGap.contains("logout")
                    || normalizedGap.contains("authenticated area")
                    || normalizedGap.contains("post-login")
                    || normalizedGap.contains("dashboard")
                    || normalizedGap.contains("dropped unsupported pom action step")
                    || normalizedGap.contains("lack explicit expected")) {
                return false;
            }
            if (normalizedGap.contains("form_visible") && hasCompositeVisibleAssertion(root, "login")) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCompositeVisibleAssertion(ObjectNode root, String hint) {
        JsonNode assertions = root.path("assertions");
        if (!assertions.isArray()) {
            return false;
        }
        for (JsonNode assertionNode : assertions) {
            String methodName = text(assertionNode, "methodName").toLowerCase(java.util.Locale.ROOT);
            if (!hint.isBlank() && !methodName.contains(hint)) {
                continue;
            }
            int visibleChecks = 0;
            for (JsonNode checkNode : assertionNode.path("checks")) {
                String check = text(checkNode, "check");
                String normalized = normalizeCheck(check);
                if ("VISIBLE".equals(normalized)) {
                    visibleChecks++;
                }
            }
            if (visibleChecks >= 2) {
                return true;
            }
        }
        return false;
    }

    private String inferRole(String id, String value) {
        String evidence = (firstNonBlank(id, "") + " " + firstNonBlank(value, "")).toLowerCase(java.util.Locale.ROOT);
        if (evidence.contains("password")) {
            return "password";
        }
        if (evidence.contains("user") || evidence.contains("email")) {
            return "input";
        }
        if (evidence.contains("button") || evidence.contains("submit") || evidence.contains("login")) {
            return "button";
        }
        if (evidence.contains("link") || evidence.contains("href")) {
            return "link";
        }
        return "unknown";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current == null ? "" : current.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
