package ua.demo.agentlab.ai.rag.intelligence.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiEndpointDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiSpecification;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenApiParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<OpenApiSpecification> parse(List<SourceDocument> documents) {
        List<OpenApiSpecification> specifications = new ArrayList<>();
        for (SourceDocument document : documents) {
            String lowerPath = document.relativePath().toLowerCase(Locale.ROOT);
            String content = document.content();
            if (!(lowerPath.endsWith(".yaml") || lowerPath.endsWith(".yml") || lowerPath.endsWith(".json"))
                    && !content.contains("openapi") && !content.contains("swagger")) {
                continue;
            }
            if ("json".equalsIgnoreCase(document.language())) {
                specifications.add(parseJson(document));
            } else if ("yaml".equalsIgnoreCase(document.language()) || "yml".equalsIgnoreCase(lowerPath)) {
                specifications.add(parseYaml(document));
            }
        }
        return specifications;
    }

    private OpenApiSpecification parseYaml(SourceDocument document) {
        List<OpenApiEndpointDefinition> endpoints = new ArrayList<>();
        String title = "";
        String currentPath = null;
        String currentMethod = null;
        String currentOperationId = "";
        String currentSummary = "";

        for (String rawLine : document.content().split("\\R")) {
            String line = rawLine.stripTrailing();
            String trimmed = line.trim();
            if (trimmed.startsWith("title:") && title.isBlank()) {
                title = trimmed.substring("title:".length()).trim().replace("\"", "");
            }
            if (trimmed.startsWith("/") && trimmed.endsWith(":")) {
                if (currentPath != null && currentMethod != null) {
                    endpoints.add(new OpenApiEndpointDefinition(
                            document.relativePath(), currentMethod, currentPath, currentOperationId, currentSummary
                    ));
                }
                currentPath = trimmed.substring(0, trimmed.length() - 1);
                currentMethod = null;
                currentOperationId = "";
                currentSummary = "";
                continue;
            }
            if (currentPath != null && trimmed.matches("(get|post|put|delete|patch):")) {
                if (currentMethod != null) {
                    endpoints.add(new OpenApiEndpointDefinition(
                            document.relativePath(), currentMethod, currentPath, currentOperationId, currentSummary
                    ));
                }
                currentMethod = trimmed.substring(0, trimmed.length() - 1).toUpperCase(Locale.ROOT);
                currentOperationId = "";
                currentSummary = "";
                continue;
            }
            if (currentMethod != null && trimmed.startsWith("operationId:")) {
                currentOperationId = trimmed.substring("operationId:".length()).trim().replace("\"", "");
            }
            if (currentMethod != null && trimmed.startsWith("summary:")) {
                currentSummary = trimmed.substring("summary:".length()).trim().replace("\"", "");
            }
        }
        if (currentPath != null && currentMethod != null) {
            endpoints.add(new OpenApiEndpointDefinition(
                    document.relativePath(), currentMethod, currentPath, currentOperationId, currentSummary
            ));
        }
        return new OpenApiSpecification(document.relativePath(), title, endpoints);
    }

    private OpenApiSpecification parseJson(SourceDocument document) {
        try {
            JsonNode root = objectMapper.readTree(document.content());
            String title = root.path("info").path("title").asText("");
            List<OpenApiEndpointDefinition> endpoints = new ArrayList<>();
            JsonNode paths = root.path("paths");
            if (paths.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> pathIterator = paths.properties().iterator();
                while (pathIterator.hasNext()) {
                    Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
                    if (!pathEntry.getValue().isObject()) {
                        continue;
                    }
                    Iterator<Map.Entry<String, JsonNode>> methodIterator = pathEntry.getValue().properties().iterator();
                    while (methodIterator.hasNext()) {
                        Map.Entry<String, JsonNode> methodEntry = methodIterator.next();
                        endpoints.add(new OpenApiEndpointDefinition(
                                document.relativePath(),
                                methodEntry.getKey().toUpperCase(Locale.ROOT),
                                pathEntry.getKey(),
                                methodEntry.getValue().path("operationId").asText(""),
                                methodEntry.getValue().path("summary").asText("")
                        ));
                    }
                }
            }
            return new OpenApiSpecification(document.relativePath(), title, endpoints);
        } catch (Exception exception) {
            return new OpenApiSpecification(document.relativePath(), "", List.of());
        }
    }
}
