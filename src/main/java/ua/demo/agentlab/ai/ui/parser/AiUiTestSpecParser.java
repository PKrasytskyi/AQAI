package ua.demo.agentlab.ai.ui.parser;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaValidator;
import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;

import java.util.ArrayList;
import java.util.List;

public class AiUiTestSpecParser extends StructuredOutputParserSupport {

    private final LlmOutputSchemaValidator schemaValidator = new LlmOutputSchemaValidator();

    public List<AiUiTestSpec> parse(String rawResponse) {
        JsonNode root = parseRoot(rawResponse);
        schemaValidator.throwIfInvalid(schemaValidator.validateUiTestSpec(root));
        List<AiUiTestSpec> specs = new ArrayList<>();
        JsonNode node = root.path("tests");
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String scenarioId = text(item, "scenarioId");
                String className = text(item, "className");
                String pageClassName = text(item, "pageClassName");
                String testMethodName = text(item, "testMethodName");
                String actionBody = text(item, "actionBody");
                String assertionBody = text(item, "assertionBody");
                if (scenarioId.isBlank() || className.isBlank() || pageClassName.isBlank()
                        || testMethodName.isBlank() || actionBody.isBlank() || assertionBody.isBlank()) {
                    continue;
                }
                specs.add(new AiUiTestSpec(
                        scenarioId,
                        className,
                        text(item, "sourcePageClassName"),
                        text(item, "sourcePageVariableName"),
                        pageClassName,
                        text(item, "pageVariableName"),
                        testMethodName,
                        text(item, "testDescription"),
                        actionBody,
                        assertionBody,
                        stringArray(item.path("additionalImports"))
                ));
            }
        }
        return List.copyOf(specs);
    }
}
