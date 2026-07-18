package ua.demo.agentlab.ai.debug;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Redacts runtime secrets from persisted AI artifacts without changing in-memory execution data. */
public final class ArtifactSecretRedactor {

    private static final Set<String> SECRET_NAMES = Set.of(
            "TEST_VALID_USERNAME", "TEST_VALID_PASSWORD", "OPENAI_API_KEY", "API_AUTH_TOKEN",
            "KNOWLEDGE_GRAPH_NEO4J_PASSWORD", "PROJECT_AUTH_USERNAME", "PROJECT_AUTH_PASSWORD"
    );

    public JsonNode redact(Object payload, ObjectMapper mapper) {
        JsonNode source = mapper.valueToTree(payload);
        return redactNode(source, false, knownSecrets());
    }

    public String redactText(String value) {
        String result = value == null ? "" : value;
        for (Map.Entry<String, String> secret : knownSecrets().entrySet()) {
            if (secret.getValue().length() >= 8) {
                result = result.replace(secret.getValue(), "${" + secret.getKey() + "}");
            }
        }
        return result;
    }

    private JsonNode redactNode(JsonNode node, boolean sensitiveContext, Map<String, String> secrets) {
        if (node == null || node.isNull()) return node;
        if (node.isObject()) {
            ObjectNode result = ((ObjectNode) node).deepCopy();
            result.fields().forEachRemaining(entry -> {
                boolean sensitive = sensitiveContext || sensitiveField(entry.getKey())
                        || "resolvedData".equalsIgnoreCase(entry.getKey());
                result.set(entry.getKey(), redactNode(entry.getValue(), sensitive, secrets));
            });
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = ((ArrayNode) node).arrayNode();
            node.forEach(item -> result.add(redactNode(item, sensitiveContext, secrets)));
            return result;
        }
        if (!node.isTextual()) return node;
        String value = node.textValue();
        if (value == null || value.matches("^\\$\\{[^}]+}$")) return node;
        if (sensitiveContext) return TextNode.valueOf("${REDACTED}");
        return secrets.entrySet().stream()
                .filter(entry -> entry.getValue().length() >= 8 && entry.getValue().equals(value))
                .findFirst()
                .<JsonNode>map(entry -> TextNode.valueOf("${" + entry.getKey() + "}"))
                .orElse(node);
    }

    private boolean sensitiveField(String fieldName) {
        String normalized = fieldName == null ? "" : fieldName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return normalized.equals("username") || normalized.equals("password")
                || normalized.contains("secret") || normalized.contains("credential")
                || normalized.equals("token") || normalized.equals("apikey");
    }

    private Map<String, String> knownSecrets() {
        Map<String, String> values = new LinkedHashMap<>();
        for (String name : SECRET_NAMES) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) value = System.getProperty(name);
            if (value != null && !value.isBlank()) values.put(name, value);
        }
        return values;
    }
}
