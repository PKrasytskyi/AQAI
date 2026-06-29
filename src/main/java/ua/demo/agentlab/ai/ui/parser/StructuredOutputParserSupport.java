package ua.demo.agentlab.ai.ui.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

abstract class StructuredOutputParserSupport {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected JsonNode parseRoot(String rawResponse) {
        try {
            return objectMapper.readTree(extractJsonObject(rawResponse));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse structured AI output", exception);
        }
    }

    protected String text(JsonNode node, String fieldName) {
        if (node == null || fieldName == null || fieldName.isBlank()) {
            return "";
        }
        JsonNode field = node.path(fieldName);
        return field.isTextual() ? field.asText().trim() : "";
    }

    protected List<String> stringArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                if (item != null && item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText().trim());
                }
            });
        }
        return List.copyOf(values);
    }

    protected <E extends Enum<E>> E parseEnum(Class<E> enumClass, String rawValue, E fallback) {
        try {
            String normalized = rawValue == null ? "" : rawValue.trim().toUpperCase(Locale.ROOT);
            return normalized.isBlank() ? fallback : Enum.valueOf(enumClass, normalized);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String extractJsonObject(String rawResponse) {
        String text = rawResponse == null ? "" : rawResponse.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }

        int start = text.indexOf('{');
        if (start < 0) {
            throw new IllegalStateException("Structured AI output does not contain a JSON object");
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, index + 1);
                }
            }
        }

        throw new IllegalStateException("Structured AI output contains incomplete JSON");
    }
}
