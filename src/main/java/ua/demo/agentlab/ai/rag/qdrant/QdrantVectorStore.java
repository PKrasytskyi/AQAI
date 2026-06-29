package ua.demo.agentlab.ai.rag.qdrant;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ai.rag.model.ArtifactType;
import ua.demo.agentlab.ai.rag.model.ChunkMetadata;
import ua.demo.agentlab.ai.rag.model.CodeChunk;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ai.rag.store.VectorStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QdrantVectorStore implements VectorStore {

    private final RagRuntimeConfig config;
    private final JsonHttpClient httpClient;

    public QdrantVectorStore(RagRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public QdrantVectorStore(RagRuntimeConfig config, JsonHttpClient httpClient) {
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
    public void ensureCollection(int vectorSize) {
        Map<String, Object> body = Map.of(
                "vectors", Map.of(
                        "size", vectorSize,
                        "distance", "Cosine"
                )
        );

        try {
            httpClient.put(
                    baseUrl() + "/collections/" + config.collectionName(),
                    body,
                    headers()
            );
        } catch (IllegalStateException exception) {
            if (isCollectionAlreadyExists(exception)) {
                return;
            }
            throw exception;
        }
    }

    @Override
    public void upsert(List<CodeChunk> chunks, List<List<Double>> vectors) {
        if (chunks.size() != vectors.size()) {
            throw new IllegalArgumentException("chunks and vectors size mismatch");
        }
        List<Map<String, Object>> points = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            CodeChunk chunk = chunks.get(index);
            points.add(Map.of(
                    "id", chunk.id(),
                    "vector", vectors.get(index),
                    "payload", chunk.payload()
            ));
        }

        httpClient.put(
                baseUrl() + "/collections/" + config.collectionName() + "/points",
                Map.of("points", points),
                headers()
        );
    }

    @Override
    public List<RetrievedChunk> search(List<Double> queryVector, int limit) {
        return search(queryVector, limit, Map.of());
    }

    @Override
    public List<RetrievedChunk> search(List<Double> queryVector, int limit, Map<String, String> mustPayloadMatch) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vector", queryVector);
        body.put("limit", limit);
        body.put("with_payload", true);
        if (mustPayloadMatch != null && !mustPayloadMatch.isEmpty()) {
            body.put("filter", payloadFilter(mustPayloadMatch));
        }
        JsonNode response = httpClient.post(
                baseUrl() + "/collections/" + config.collectionName() + "/points/search",
                body,
                headers()
        );

        List<RetrievedChunk> matches = new ArrayList<>();
        JsonNode result = response.path("result");
        if (!result.isArray()) {
            return matches;
        }

        result.forEach(item -> {
            JsonNode payload = item.path("payload");
            matches.add(new RetrievedChunk(
                    item.path("id").asText(),
                    payload.path("relativePath").asText("unknown"),
                    payload.path("language").asText("unknown"),
                    payload.path("chunkIndex").asInt(-1),
                    item.path("score").asDouble(0.0d),
                    payload.path("text").asText(""),
                    new ChunkMetadata(
                            parseArtifactType(payload.path("artifactType").asText("UNKNOWN")),
                            payload.path("artifactName").asText(""),
                            payload.path("packageName").asText(""),
                            parseTags(payload.path("tags"))
                    )
            ));
        });
        return matches;
    }

    private Map<String, Object> payloadFilter(Map<String, String> mustPayloadMatch) {
        List<Map<String, Object>> must = new ArrayList<>();
        for (Map.Entry<String, String> entry : mustPayloadMatch.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()
                    || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            must.add(Map.of(
                    "key", entry.getKey(),
                    "match", Map.of("value", entry.getValue())
            ));
        }
        return Map.of("must", must);
    }

    private ArtifactType parseArtifactType(String value) {
        try {
            return ArtifactType.valueOf(value);
        } catch (Exception ignored) {
            return ArtifactType.UNKNOWN;
        }
    }

    private List<String> parseTags(JsonNode tagsNode) {
        if (tagsNode == null || !tagsNode.isArray()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        tagsNode.forEach(node -> {
            if (node != null && node.isTextual() && !node.asText().isBlank()) {
                tags.add(node.asText());
            }
        });
        return tags.isEmpty() ? Collections.emptyList() : List.copyOf(tags);
    }

    private String baseUrl() {
        String value = config.qdrantUrl();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        if (config.qdrantApiKey() != null && !config.qdrantApiKey().isBlank()) {
            headers.put("api-key", config.qdrantApiKey());
        }
        return headers;
    }

    private boolean isCollectionAlreadyExists(IllegalStateException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("status 409")
                && normalized.contains("already exists");
    }
}
