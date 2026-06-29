package ua.demo.agentlab.ai.ui.parser;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaValidator;
import ua.demo.agentlab.ai.ui.model.AiLocatorSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodSpec;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;

import java.util.ArrayList;
import java.util.List;

public class AiPageObjectSpecParser extends StructuredOutputParserSupport {

    private final LlmOutputSchemaValidator schemaValidator = new LlmOutputSchemaValidator();

    public List<AiPageObjectSpec> parse(String rawResponse) {
        JsonNode root = parseRoot(rawResponse);
        schemaValidator.throwIfInvalid(schemaValidator.validatePageObjectSpec(root));
        List<AiPageObjectSpec> specs = new ArrayList<>();
        JsonNode node = root.path("pageObjects");
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String pageName = text(item, "pageName");
                if (pageName.isBlank()) {
                    continue;
                }
                specs.add(new AiPageObjectSpec(
                        pageName,
                        text(item, "route"),
                        text(item, "openMethodName"),
                        parseLocators(item.path("locators")),
                        parseMethods(item.path("methods"))
                ));
            }
        }
        return List.copyOf(specs);
    }

    private List<AiLocatorSpec> parseLocators(JsonNode node) {
        List<AiLocatorSpec> specs = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String fieldName = text(item, "fieldName");
                String strategy = text(item, "strategy");
                String value = text(item, "value");
                if (fieldName.isBlank() || strategy.isBlank() || value.isBlank()) {
                    continue;
                }
                specs.add(new AiLocatorSpec(fieldName, text(item, "elementName"), strategy, value));
            }
        }
        return List.copyOf(specs);
    }

    private List<AiMethodSpec> parseMethods(JsonNode node) {
        List<AiMethodSpec> specs = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String methodName = text(item, "methodName");
                String body = text(item, "body");
                if (methodName.isBlank() || body.isBlank()) {
                    continue;
                }
                specs.add(new AiMethodSpec(
                        defaultIfBlank(text(item, "returnType"), "void"),
                        methodName,
                        parseParameters(item.path("parameters")),
                        body,
                        stringArray(item.path("requiredImports"))
                ));
            }
        }
        return List.copyOf(specs);
    }

    private List<AiMethodParameterSpec> parseParameters(JsonNode node) {
        List<AiMethodParameterSpec> parameters = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String type = text(item, "type");
                String name = text(item, "name");
                if (!type.isBlank() && !name.isBlank()) {
                    parameters.add(new AiMethodParameterSpec(type, name));
                }
            }
        }
        return List.copyOf(parameters);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
