package ua.demo.agentlab.ai.rag.openai;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.embedding.EmbeddingService;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiEmbeddingClient implements EmbeddingService {

    private final RagRuntimeConfig config;
    private final JsonHttpClient httpClient;

    public OpenAiEmbeddingClient(RagRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public OpenAiEmbeddingClient(RagRuntimeConfig config, JsonHttpClient httpClient) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient cannot be null");
        }
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public List<Double> embed(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("input cannot be blank");
        }
        JsonNode response = httpClient.post(
                trimTrailingSlash(config.openAiBaseUrl()) + "/embeddings",
                Map.of(
                        "model", config.embeddingModel(),
                        "input", input
                ),
                defaultHeaders()
        );

        JsonNode embeddingNode = response.path("data").path(0).path("embedding");
        if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new IllegalStateException("OpenAI embeddings response does not contain a vector");
        }

        List<Double> vector = new java.util.ArrayList<>(embeddingNode.size());
        embeddingNode.forEach(value -> vector.add(value.asDouble()));
        return vector;
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

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
