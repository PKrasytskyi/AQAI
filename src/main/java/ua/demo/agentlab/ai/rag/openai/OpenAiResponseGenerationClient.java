package ua.demo.agentlab.ai.rag.openai;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.openai.OpenAiTokenUsage;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;

import java.util.LinkedHashMap;
import java.util.Map;

public class OpenAiResponseGenerationClient {

    private final RagRuntimeConfig config;
    private final JsonHttpClient httpClient;
    private OpenAiTokenUsage lastUsage = OpenAiTokenUsage.EMPTY;

    public OpenAiResponseGenerationClient(RagRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public OpenAiResponseGenerationClient(RagRuntimeConfig config, JsonHttpClient httpClient) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient cannot be null");
        }
        this.config = config;
        this.httpClient = httpClient;
    }

    public String generate(String prompt) {
        return generateResponse(prompt);
    }

    public String generateResponse(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt cannot be blank");
        }

        JsonNode response = httpClient.post(
                trimTrailingSlash(config.openAiBaseUrl()) + "/responses",
                Map.of(
                        "model", config.generationModel(),
                        "input", prompt,
                        "max_output_tokens", config.maxOutputTokens(),
                        "reasoning", Map.of("effort", "medium"),
                        "text", Map.of("format", Map.of("type", "text"), "verbosity", "low"),
                        "truncation", "auto"
                ),
                defaultHeaders()
        );
        lastUsage = tokenUsage(response);

        String rawResponse = response.toPrettyString();
        String outputText = findOutputText(response);
        if (outputText == null || outputText.isBlank()) {
            throw new OpenAiEmptyOutputException(
                    "OpenAI responses API returned no output text. " + summarizeResponse(response),
                    rawResponse
            );
        }

        return outputText.trim();
    }

    public OpenAiTokenUsage lastUsage() {
        return lastUsage;
    }

    private String findOutputText(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return null;
        }

        JsonNode outputTextNode = root.get("output_text");
        if (outputTextNode != null && outputTextNode.isTextual() && !outputTextNode.asText().isBlank()) {
            return outputTextNode.asText();
        }

        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : output) {
                JsonNode content = item.get("content");
                if (content == null || !content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    String type = contentItem.path("type").asText("");
                    String text = extractContentText(contentItem, type);
                    if (!text.isBlank()) {
                        if (!builder.isEmpty()) {
                            builder.append(System.lineSeparator());
                        }
                        builder.append(text);
                    }
                }
            }
            return builder.isEmpty() ? null : builder.toString();
        }

        return null;
    }

    private String extractContentText(JsonNode contentItem, String type) {
        if (contentItem == null) {
            return "";
        }

        if ("output_text".equals(type) || "text".equals(type)) {
            JsonNode textNode = contentItem.get("text");
            if (textNode != null) {
                if (textNode.isTextual()) {
                    return textNode.asText("");
                }
                if (textNode.isObject()) {
                    String value = textNode.path("value").asText("");
                    if (!value.isBlank()) {
                        return value;
                    }
                }
            }
            String value = contentItem.path("value").asText("");
            if (!value.isBlank()) {
                return value;
            }
        }

        if ("refusal".equals(type)) {
            return contentItem.path("refusal").asText("");
        }

        return "";
    }

    private String summarizeResponse(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return "Response body is empty.";
        }

        String status = root.path("status").asText("");
        String incompleteReason = root.path("incomplete_details").path("reason").asText("");
        String errorMessage = root.path("error").path("message").asText("");
        String outputTypes = collectOutputTypes(root);

        StringBuilder summary = new StringBuilder();
        if (!status.isBlank()) {
            summary.append("status=").append(status).append(". ");
        }
        if (!incompleteReason.isBlank()) {
            summary.append("incomplete_reason=").append(incompleteReason).append(". ");
        }
        if (!outputTypes.isBlank()) {
            summary.append("output_types=").append(outputTypes).append(". ");
        }
        if (!errorMessage.isBlank()) {
            summary.append("error=").append(errorMessage).append(". ");
        }

        return summary.isEmpty() ? "No diagnostic fields were present in the response." : summary.toString().trim();
    }

    private String collectOutputTypes(JsonNode root) {
        JsonNode output = root.path("output");
        if (!output.isArray()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                String type = contentItem.path("type").asText("");
                if (!type.isBlank()) {
                    if (!builder.isEmpty()) {
                        builder.append(",");
                    }
                    builder.append(type);
                }
            }
        }
        return builder.toString();
    }

    private Map<String, String> defaultHeaders() {
        String apiKey = config.openAiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured for RAG");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        return headers;
    }

    private OpenAiTokenUsage tokenUsage(JsonNode root) {
        JsonNode usage = root == null ? null : root.path("usage");
        if (usage == null || usage.isMissingNode()) {
            return OpenAiTokenUsage.EMPTY;
        }
        int inputTokens = firstInt(usage, "input_tokens", "prompt_tokens");
        int outputTokens = firstInt(usage, "output_tokens", "completion_tokens");
        int totalTokens = firstInt(usage, "total_tokens", "totalTokens");
        return new OpenAiTokenUsage(inputTokens, outputTokens, totalTokens);
    }

    private int firstInt(JsonNode node, String... fields) {
        if (node == null) {
            return 0;
        }
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.canConvertToInt()) {
                return value.asInt();
            }
        }
        return 0;
    }

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
